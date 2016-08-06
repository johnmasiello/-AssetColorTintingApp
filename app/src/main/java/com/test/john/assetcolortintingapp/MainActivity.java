package com.test.john.assetcolortintingapp;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;

import static com.test.john.assetcolortintingapp.MainActivity.Channel.*;

public class MainActivity extends AppCompatActivity {

    enum Channel {
        ALPHA, RED, GREEN, BLUE
    }

    private EnumMap<Channel, SeekBar> seekBars;
    private EnumMap<Channel, Point> toastOffsets;
    private ListView listView;
    private ArrayAdapter<String> listAdapter;
    private ArrayList<Bitmap> imageAssets;
    private ArrayList<String> filenames;
    private EditText editText;
    private ImageView image;

    private String hex_color_reset_string;
    private String hex_color_reset_with_full_opacity;

    private int gravity;
    private static final float TOAST_MARGIN_HORIZONTAL_LAND = 0.07f; // as percentage of screen
    private static final float TOAST_MARGIN_HORIZONTAL_PORT = 0; // as percentage of screen
    private float toastMarginHorizontal;

    /** helper variables */
    private int filenameIndex = -1;
    private static final String FILENAME_INDEX_KEY = "file_name_index";
    private static final String HEX_COLOR_KEY = "hex_color";
    private Toast toast, openFileFailureToast;

    private static final int IMAGE_SELECTED = 22;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hex_color_reset_string = getResources().getString(R.string.hexColorDefaultText);
        hex_color_reset_with_full_opacity = hex_color_reset_string.charAt(0)+"FF"+
                hex_color_reset_string.substring(3, 9);

        listView = (ListView) findViewById(R.id.select_dialog_listview);
        image = (ImageView) findViewById(R.id.image);
        editText = (EditText) findViewById(R.id.hex_input_box);

        toastOffsets = new EnumMap<>(Channel.class);
        setToastOnSlidePosition();


        setHardwareAccelerated(image, false);

        /** get holders to all of the seekBars and add listeners */
        setSeekbars();


        imageAssets = new ArrayList<>();
        filenames = new ArrayList<>();
        listAdapter = new ArrayAdapter<>(this, R.layout.list_row, filenames);
        listView.setAdapter(listAdapter);

        if (savedInstanceState != null) {

            /** recover the color settings */
            String colorString = savedInstanceState.getString(HEX_COLOR_KEY);
            editText.setText(colorString);
            applyEditTextFormatting(colorString);

            /** recover the image */
            String filename = savedInstanceState.getString(FILENAME_INDEX_KEY);

//            if (filename != null) {
//
//                int i = 0;
//                for (String s : filenames) {
//                    if (s.equals(filename)) {
//                        filenameIndex = i;
//                        applyColorFilterUsingPorterDuffMode();
//                        break;
//                    }
//                    i++;
//                }
//            }
        }

        setOnClickListenerForListView();

        /** set a color pattern on the hex color text */
        CharSequence currentText = editText.getText();
        applyEditTextFormatting(currentText);

        /** set a listener on the editText for done typing */
        setDoneTypingListener();

        editText.setSingleLine(true);

        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean focused) {
                InputMethodManager keyboard = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (focused) {
                    keyboard.showSoftInput(editText, 0);

                    // adjust layout
//                    editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.hexStringTextSizeCondensed));
                }
                else {
                    keyboard.hideSoftInputFromWindow(editText.getWindowToken(), 0);

                    // adjust layout
//                    editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.hexColorTextSize));
                }
            }
        });
    }

    public static void setHardwareAccelerated(View view, boolean enabled){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
            if(enabled)
                view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            else view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    /** <p>load svgs</p>
     *  <p>load file names</p>
     *
     */
    private void getImageAssets() {
        imageAssets = new ArrayList<>();

        AssetManager assetManager = getAssets();
        String[] filenames;
        String fullPath = null;
        String path = "stock_images";

        try {
            filenames = assetManager.list(path);
        }
        catch (IOException e) {
            throw new RuntimeException("could not load assets");
        }

        InputStream bitmapIn = null;
        Bitmap b;

        for (String s : filenames) {
            try {

                fullPath = path + "/" + s;

                bitmapIn = assetManager.open(fullPath);
                b = BitmapFactory.decodeStream(bitmapIn);

                if (b == null) {
                    throw new IOException();
                } else {
                    imageAssets.add(b);
                }

            } catch (IOException e) {
                e.printStackTrace();
                Log.v("DEBUG", "fullPath");
                throw new RuntimeException("could not load assets");
            }
            finally {
                try {
                    bitmapIn.close();
                }
                catch (IOException ioe) {
                    ioe.printStackTrace();
                    Log.v("DEBUG", "unable to close filestream for " + fullPath);
                }
                catch (NullPointerException np) {
                    np.printStackTrace();
                }
            }
        }

        this.filenames = new ArrayList<>(Arrays.asList(filenames));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.reset_color) {

            editText.setText(hex_color_reset_string);
            applyColorFilterUsingPorterDuffMode();
            return true;
        } else if (id == R.id.openImage) {

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");

            // Do this if you need to be able to open the returned URI as a stream
            // (for example here to read the image data).
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            Intent finalIntent = Intent.createChooser(intent, "Select image");

            startActivityForResult(finalIntent, IMAGE_SELECTED);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == IMAGE_SELECTED) {
            if(resultCode == Activity.RESULT_OK){

                InputStream inputStream = null;

                int indexOfFirstFileAdded = imageAssets.size();

                try {

                    Uri uri = data.getData();
                    inputStream = getContentResolver().openInputStream(uri);

                    Bitmap b = BitmapFactory.decodeStream(inputStream);

                    if (b == null) {
                        Log.v("DEBUG", "Error decoding file");

                    } else {

                        imageAssets.add(b);

                        // Get the filename associated with the inputStream, from the Uri
                        Cursor returnCursor = getContentResolver().query(uri, null, null, null, null);
                        if (returnCursor != null) {

                            int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                            returnCursor.moveToFirst();

                            filenames.add(returnCursor.getString(nameIndex));
                            returnCursor.close();
                        }
                    }

                    // Display the first file that was just added, or if a single file was opened, just that one
                    filenameIndex = indexOfFirstFileAdded;
                    listAdapter.notifyDataSetInvalidated();
                    applyColorFilterUsingPorterDuffMode();
                }
                catch (FileNotFoundException e) {
                    e.printStackTrace();
                    openFileFailureToast = Toast.makeText(getApplicationContext(), "File could not be opened", Toast.LENGTH_SHORT);
                    openFileFailureToast.show();
                }
                finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                Log.v("DEBUG", "file chooser was canceled");
            }
        }
    }//onActivityResult

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(HEX_COLOR_KEY, editText.getText().toString());
        outState.putString(FILENAME_INDEX_KEY,
                filenameIndex != -1 && filenameIndex < filenames.size() ? filenames.get(filenameIndex) : null);
    }

    @Override
    public void onStop() {
        super.onStop();

        if (openFileFailureToast!=null)  openFileFailureToast.cancel();
    }

    /** <p>Precondtion: set filenameIndex, set editText</p>
     *
     */
    public void applyColorFilterUsingPorterDuffMode() {

        CharSequence c = editText.getText();
        Log.d("DEBUG", "edit text string content = "+c);

        if (c.length() == 9) {
            int color, alpha, red, green, blue;
            alpha = red = green = blue = 0;

            /** filter user input for each channel */
            try {
                alpha = Integer.valueOf(c.subSequence(1, 3).toString(), 16);
                seekBars.get(ALPHA).setProgress(alpha);
            }
            catch (NumberFormatException n) {
                n.printStackTrace();
            }
            try {
                red = Integer.valueOf(c.subSequence(3, 5).toString(), 16);
                seekBars.get(RED).setProgress(red);
            }
            catch (NumberFormatException n) {
                n.printStackTrace();
            }
            try {
                green = Integer.valueOf(c.subSequence(5, 7).toString(), 16);
                seekBars.get(GREEN).setProgress(green);
            }
            catch (NumberFormatException n) {
                n.printStackTrace();
            }
            try {
                blue = Integer.valueOf(c.subSequence(7, 9).toString(), 16);
                seekBars.get(BLUE).setProgress(blue);
            }
            catch (NumberFormatException n) {
                n.printStackTrace();
            }

            color = Color.argb(alpha, red, green, blue);

            Log.v("DEBUG", "color = " + color);

            /** reformat the text in the editText */
            c = "#" +  (alpha < 0x10 ? '0' : "") + Integer.toHexString(alpha).toUpperCase() +
                    (red   < 0x10 ? '0' : "") + Integer.toHexString(red).toUpperCase() +
                    (green < 0x10 ? '0' : "") + Integer.toHexString(green).toUpperCase()+
                    (blue  < 0x10 ? '0' : "") + Integer.toHexString(blue).toUpperCase();

            Log.v("DEBUG", "new color string " + c);
            editText.setText(c);
            applyEditTextFormatting(c);

            if (filenameIndex!=-1) {

                Bitmap bitmap = imageAssets.get(filenameIndex);
                BitmapDrawable drawable = new BitmapDrawable(bitmap);

                drawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
//            ColorMatrix colorMatrix = new ColorMatrix();
//            colorMatrix.setRotate(0, 90);
//
//            drawable.setColorFilter(new ColorMatrixColorFilter(colorMatrix));

                image.setImageDrawable(drawable);
            }
        }
    }

    private void setDoneTypingListener() {
        editText.setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                                actionId == EditorInfo.IME_ACTION_DONE ||
                                actionId == EditorInfo.IME_ACTION_NEXT ||
                                event.getAction() == KeyEvent.ACTION_DOWN &&
                                        event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                            if (event == null || !event.isShiftPressed()) {
                                // the user is done typing.

                                applyColorFilterUsingPorterDuffMode();
                                editText.clearFocus();
                                return true; // consume.
                            }
                        }
                        return false; // pass on to other listeners.
                    }
                });
    }

    private void applyEditTextFormatting(CharSequence inputText) {
        if (inputText.length() == 9) {

            String text = inputText.charAt(0) + "<font color='grey'>"+inputText.subSequence(1, 3)+
                    "</font><font color='red'>"+inputText.subSequence(3,5)+
                    "</font><font color='green'>"+inputText.subSequence(5,7)+
                    "</font><font color='blue'>"+inputText.subSequence(7,9)+"</font>";

            editText.setText(Html.fromHtml(text), TextView.BufferType.SPANNABLE);

            Log.d("DEBUG", "edit text with HTML tags= " + text);
        }
        Log.v("DEBUG", "length of text = " + editText.getText().length());
    }

    private void setOnClickListenerForListView() {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                filenameIndex = position;

                if (editText.getText().length() != 9)
                    editText.setText(hex_color_reset_string);

                applyColorFilterUsingPorterDuffMode();
            }
        });
    }

    private void setSeekbars() {

        seekBars = new EnumMap<>(Channel.class);

        seekBars.put(ALPHA, (SeekBar) findViewById(R.id.seekBar));
        seekBars.put(RED, (SeekBar) findViewById(R.id.seekBar2));
        seekBars.put(GREEN, (SeekBar) findViewById(R.id.seekBar3));
        seekBars.put(BLUE, (SeekBar) findViewById(R.id.seekBar4));

        for (Channel s : seekBars.keySet())
            setOnSeekBarChangeListener(s);
    }

    private void setOnSeekBarChangeListener(final Channel channel) {

        SeekBar seekBar = seekBars.get(channel);

        seekBar.setMax(255);

        final Context context = this;
        final MainActivity mainActivity = this;

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (!fromUser) return;

                    /** display toast for level of channel */
                    if (toast != null) toast.cancel();
                    toast = Toast.makeText(context, Integer.toString(progress),
                            Toast.LENGTH_SHORT);

                    // set its offsets and margins
                    Point offset = toastOffsets.get(channel);
                    toast.setGravity(gravity, offset.x, offset.y);
                    toast.setMargin(toastMarginHorizontal, 0);

                    toast.show();

                    /** update the editText with hex color */
                    // not supposed to modify charsequence in editText from the getText call
                    String hex = editText.getText().toString();

                    if (hex.length() != 9)
                        hex = hex_color_reset_with_full_opacity;

                    hex = mainActivity.updateHexColorOnSlide(hex, progress, channel);
                    hex = hex.toUpperCase();

                    editText.setText(hex);

                    /** render the graphic */
                    applyColorFilterUsingPorterDuffMode();
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
    }

    private String updateHexColorOnSlide(String hexColor, int position, Channel channel) {
        switch (channel) {
            case ALPHA:
                return hexColor.charAt(0)+String.format("%02x", position)+
                        hexColor.substring(3,9);
            case RED:
                return hexColor.substring(0, 3)+String.format("%02x", position)+
                        hexColor.substring(5, 9);
            case GREEN:
                return hexColor.substring(0, 5)+String.format("%02x", position)+
                        hexColor.substring(7, 9);
            case BLUE:
                return hexColor.substring(0, 7)+String.format("%02x", position);
        }
        return hex_color_reset_string;
    }

    private void setToastOnSlidePosition() {

        Display display = ((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);

        int height_Screen, width_Screen, screen_rotate;

        height_Screen = displayMetrics.heightPixels;
        width_Screen = displayMetrics.widthPixels;


        screen_rotate = display.getRotation();

        /** 0 degrees rotation is vertically oriented upward */
        switch (screen_rotate) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                gravity = Gravity.CENTER;

                toastOffsets.put(ALPHA, new Point(0, (int) (-.055 * height_Screen)));
                toastOffsets.put(RED, new Point(0, (int) (.01 * height_Screen)));
                toastOffsets.put(GREEN, new Point(0, (int) (.075 * height_Screen)));
                toastOffsets.put(BLUE, new Point(0, (int) (.14 * height_Screen)));

                toastMarginHorizontal = TOAST_MARGIN_HORIZONTAL_PORT;
                break;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;

                toastOffsets.put(ALPHA, new Point(0, (int) (-.16 * height_Screen)));
                toastOffsets.put(RED, new Point(0, (int) (-.02 * height_Screen)));
                toastOffsets.put(GREEN, new Point(0, (int) (.13 * height_Screen)));
                toastOffsets.put(BLUE, new Point(0, (int) (.28 * height_Screen)));

                toastMarginHorizontal = TOAST_MARGIN_HORIZONTAL_LAND;
        }

    }
}
