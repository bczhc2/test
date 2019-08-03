package com.zhc.tools;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import filepicker.Picker;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;
import static com.zhc.utils.DisplayUtil.px2sp;

public class Settings extends AppCompatActivity {
    private Intent r_intent = new Intent();
    private LinearLayout ll;
    private File file;
    private boolean haveChange = false;
    private CodecsActivity o = new CodecsActivity();
    private String[] jsonText = new String[]{
            "sourceExtension",
            "destExtension",
            "deleteOldFile"

    };
    private String[] dT;
    private List<List<View>> lists;
    private JSONObject json;
    private CountDownLatch latch;
    private CountDownLatch latch1;
    private List<List<String>> saved;
    private String savedJSONText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        ll = findViewById(R.id.ll);
        ExecutorService es = Executors.newCachedThreadPool();
        this.latch = new CountDownLatch(1);
        this.latch1 = new CountDownLatch(1);
        es.execute(() -> {
            try {
                this.file = this.o.getFile();
                if (!file.exists()) System.out.println("file.createNewFile() = " + file.createNewFile());
            } catch (IOException e) {
                new Picker().showException(e, Settings.this);
            }
            this.lists = new ArrayList<>();
            this.json = new JSONObject();
            Intent intent = this.getIntent();
            ArrayList<String> optionsList = intent.getStringArrayListExtra("options");
            this.savedJSONText = intent.getStringExtra("jsonText");
            this.dT = optionsList.toArray(new String[0]);
            this.latch1.countDown();
            int length = this.dT.length;
            LinearLayout[] linearLayouts = new LinearLayout[length];
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            int[] color = new int[]{
                    ContextCompat.getColor(this, R.color.s1),
                    ContextCompat.getColor(this, R.color.s2),
            };
            int[] textViewsText = new int[]{
                    R.string.filter,
                    R.string.iFileExtension,
                    R.string.oFileExtension
            };
            for (int i = 0; i < length; i++) {
                int finalI = i;
                linearLayouts[i] = new LinearLayout(this);
                linearLayouts[i].setOrientation(LinearLayout.VERTICAL);
                linearLayouts[i].setLayoutParams(lp);
                linearLayouts[i].setBackgroundColor(color[i & 1]);
                TextView option_tv = new TextView(this);
                option_tv.setGravity(Gravity.CENTER);
                option_tv.setText(this.dT[i]);
                option_tv.setTextSize(25F);
//                EditText path_et = new EditText(this);
                EditText[] editTexts = new EditText[2];
                TextView[] textViews = new TextView[3];
                for (int j = 0; j < textViews.length; j++) {
                    textViews[j] = new TextView(this);
                    textViews[j].setText(textViewsText[j]);
                }
                List<View> viewList = new ArrayList<>();
                try {
                    this.latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                for (int j = 0; j < editTexts.length; j++) {
                    editTexts[j] = new EditText(this);
                    int finalJ = j;
                    runOnUiThread(() -> {
                        String s = null;
                        try {
                            s = this.saved.get(finalI).get(finalJ);
                        } catch (IndexOutOfBoundsException ignored) {
                        }
                        editTexts[finalJ].setText(s == null ? "" : s);
                    });
                    editTexts[j].setOnFocusChangeListener((v, hasFocus) -> this.haveChange = true);
                    viewList.add(editTexts[j]);
                }
                float textSize = px2sp(Settings.this, textViews[1].getTextSize());
                textViews[0].setTextSize(textSize + 5);
                CheckBox[] checkBoxes = new CheckBox[length];
                CountDownLatch latch = new CountDownLatch(1);
                runOnUiThread(() -> {
                    linearLayouts[finalI].addView(option_tv);
                    linearLayouts[finalI].addView(textViews[0]);
                    for (int j = 0; j < 2; j++) {
                        linearLayouts[finalI].addView(textViews[j + 1]);
                        linearLayouts[finalI].addView(editTexts[j]);
                    }
                    checkBoxes[finalI] = new CheckBox(this);
                    checkBoxes[finalI].setText(R.string.delete_old_file);
                    checkBoxes[finalI].setOnClickListener(v -> Snackbar.make(ll, String.valueOf(checkBoxes[finalI].isChecked()), Snackbar.LENGTH_SHORT).show());
                    linearLayouts[finalI].addView(checkBoxes[finalI]);
                    ll.addView(linearLayouts[finalI]);
                    latch.countDown();
                });
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                viewList.add(checkBoxes[i]);
                this.lists.add(viewList);
            }
            RelativeLayout rl = new RelativeLayout(this);
            rl.setGravity(Gravity.CENTER);
            RelativeLayout.LayoutParams rl_lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            rl.setLayoutParams(rl_lp);
            FloatingActionButton fab = new FloatingActionButton(this);
            fab.setOnClickListener(v -> {
                saveAll();
                String s = null;
                try {
                    s = this.json.toString();
                    saveJSONText(s);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                this.r_intent.putExtra("result", s);
                returnIntent(this.r_intent);
            });
            runOnUiThread(() -> {
                rl.addView(fab);
                ll.addView(rl);
            });
        });

        es.execute(() -> {
            try {
                latch1.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Thread.currentThread() = " + Thread.currentThread());
            try {
//                this.o.spinnerData.addAll(Arrays.asList(this.dT));
                this.o.spinnerData = new ArrayList<>();
                Collections.addAll(this.o.spinnerData, this.dT);
                this.saved = this.o.solveJSON(this.savedJSONText);
            } catch (JSONException e) {
                e.printStackTrace();
                this.runOnUiThread(() -> makeText(this, this.getString(R.string.json_solve_error) + e.toString(), LENGTH_SHORT).show());
                try {
                    OutputStream os = new FileOutputStream(file, false);
                    os.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            this.latch.countDown();
        });
        es.shutdown();
    }

    private void returnIntent(Intent intent) {
        this.setResult(4, intent);
        this.finish();
    }

    @Override
    public void onBackPressed() {
        if (this.haveChange) {
            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            adb.setTitle("是否保存")
                    .setNegativeButton("否", (dialog, which) -> this.finish())
                    .setPositiveButton("是", (dialog, which) -> {
                        /*List<IIOObject> list = this.iio.list;
                        for (IIOObject iioObject : list) {
                            String s = ((EditText) iioObject.o).getText().toString();
                            String name = this.jsonText[iioObject.key1][iioObject.key2];
                            boolean isNull = units[iioObject.key1].isNull(name);
                            try {
                                if (isNull || units[iioObject.key1].get(name).equals(""))
                                    units[iioObject.key1].put(name, s);
                            } catch (JSONException e) {
                                e.printStackTrace();
                                if (isNull) {
                                    try {
                                        units[iioObject.key1].put(name, s);
                                    } catch (JSONException ex) {
                                        ex.printStackTrace();
                                    }
                                }
                                Snackbar snackbar = Snackbar.make(ll, e.toString(), Snackbar.LENGTH_SHORT);
                                snackbar.setAction("×", v -> snackbar.dismiss()).show();
                            }
                        }*/
                        /*for (IIOObject iioObject : list) {
                            units[iioObject.key1][iioObject.key2] = new JSONObject();
                            try {
                                if ((iioObject.key2 & 1) == 0) {
                                    units[iioObject.key1][iioObject.key2].put(jsonText[iioObject.key1][iioObject.key2], ((EditText) iioObject.o).getText().toString());
                                } else {
                                    units[iioObject.key1][iioObject.key2].put(jsonText[iioObject.key1][iioObject.key2], ((EditText) iioObject.o).getText().toString());
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Snackbar snackbar = Snackbar.make(ll, e.toString(), Snackbar.LENGTH_SHORT);
                                snackbar.setAction("×", v -> snackbar.dismiss()).show();
                            }
                        }*/
                        /*try {
                            OutputStream os = new FileOutputStream(this.file);
                            OutputStreamWriter osw = new OutputStreamWriter(os);
                            BufferedWriter bw = new BufferedWriter(osw);

                            bw.close();
                            osw.close();
                            os.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }*/
                        saveAll();
                        String r = this.json.toString();
                        this.r_intent.putExtra("result", r);
                        try {
                            saveJSONText(r);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        this.returnIntent(r_intent);
                    })
                    .show();
        } else super.onBackPressed();
    }

//    /**
//     * Generate a value suitable for use in .
//     * This value will not collide with ID values generated at build time by aapt for R.id.
//     *
//     * @return a generated ID value
//     */
    /*private int generateViewId() {
        final AtomicInteger sNextGeneratedId = new AtomicInteger(1);
        for (; ; ) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }*/

    private void saveAll() {
        try {
            for (int i = 0; i < this.lists.size(); i++) {
                JSONObject o = new JSONObject();
                for (int j = 0; j < /*this.lists.get(i).size() - 1*/ 2; j++) {
                    o.put(this.jsonText[j], ((EditText) this.lists.get(i).get(j)).getText().toString());
                }
                o.put(this.jsonText[2], ((CheckBox) this.lists.get(i).get(2)).isChecked());
                this.json.put(this.dT[i], o);
            }
        } catch (JSONException e) {
            new Picker().showException(e, this);
        }
    }

    private void saveJSONText(String content) throws IOException {
        OutputStream os = new FileOutputStream(this.file);
        OutputStreamWriter osw = new OutputStreamWriter(os);
        BufferedWriter bw = new BufferedWriter(osw);
        bw.write(content);
        bw.flush();
        bw.close();
        osw.close();
        os.close();
    }
}

/*
class IIO {
    private List<IIOObject> list;

    IIO() {
        list = new ArrayList<>();
    }

    @SuppressWarnings("UnusedReturnValue")
    IIO put(int key1, int key2, Object o) {
        list.add(new IIOObject(key1, key2, o));
        return this;
    }

    */
/*Object get(int key1, int key2) {
        for (IIOObject iisObject : list) {
            if (iisObject.key1 == key1 && iisObject.key2 == key2) {
                return iisObject.o;
            }
        }
        return null;
    }*//*


    int length() {
        return list.size();
    }
}

class IIOObject {
    private int key1, key2;
    private Object o;

    IIOObject(int key1, int key2, Object o) {
        this.key1 = key1;
        this.key2 = key2;
        this.o = o;
    }
}*/