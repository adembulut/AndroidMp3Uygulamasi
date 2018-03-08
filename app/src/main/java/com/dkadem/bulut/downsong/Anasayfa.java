package com.dkadem.bulut.downsong;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dkadem.bulut.downsong.adapters.SongAdapter;
import com.dkadem.bulut.downsong.nesneler.Dosyalar;
import com.dkadem.bulut.downsong.nesneler.Song;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.logging.Handler;

public class Anasayfa extends AppCompatActivity {


    ListView listKategoriler;
    ListView listSong;
    DrawerLayout drawer;
    Toolbar toolbar;
    EditText editAranacak;
    ImageView imageSearch;
    RelativeLayout relativMediaPlayer;
    ImageView imagePlayPause;
    ImageView imageStop;
    ImageView imageSound;
    String currentListenUrl = "";
    DownloadManager manager;
    DownloadManager.Request request;
    MediaPlayer mPlayer;
    InterstitialAd mInterstitialAd;
    SharedPreferences preferences;
    TextView textBuffer;
    TextView textSanatci_SarkiAdi;
    SeekBar seekBar;
    String paylasLink;

    private android.os.Handler turkHandler = new android.os.Handler();
    private Runnable UpdateSongTime = new Runnable() {
        @Override
        public void run() {
            if (mPlayer != null) {
                int mCurrentPosition = mPlayer.getCurrentPosition() / 1000;
                seekBar.setProgress(mCurrentPosition);
                turkHandler.postDelayed(this, 1000);

            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlayer.reset();
        mPlayer.release();
    }

    public static boolean InternetVarMi(Activity activity) {
        ConnectivityManager manager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager.getActiveNetworkInfo() != null && manager.getActiveNetworkInfo().isAvailable()
                && manager.getActiveNetworkInfo().isConnected()) {
            return true;

        } else
            return false;
    }

    void yazilariYaz() {

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("Şarkı indir");
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        listKategoriler = (ListView) findViewById(R.id.listKategoriler);
        listSong = (ListView) findViewById(R.id.listSongs);
        editAranacak = (EditText) findViewById(R.id.editAranacak);
        imageSearch = (ImageView) findViewById(R.id.imageSearch);
        mediaIconlari();
    }

    void mediaIconlari() {
        imagePlayPause = (ImageView) findViewById(R.id.btnPlay);
        imageStop = (ImageView) findViewById(R.id.btnStop);
        imageSound = (ImageView) findViewById(R.id.btnSound);
        textBuffer = (TextView) findViewById(R.id.textSonBuffering);
        textSanatci_SarkiAdi = (TextView) findViewById(R.id.textSonSanatciVeSarkiAdi);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
    }

    public boolean isYazmaIzni(Context ctx) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ctx.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                Log.e("izin", "problem çıkardı");
                final Activity activity = (Activity) ctx;

                AlertDialog dialog = new AlertDialog.Builder(ctx)
                        .setTitle("Uygulama izin isteği")
                        .setMessage("Şarkıları kaydedebilmek için dosyalarınıza erişim iznine ihtiyacımız var. Bir sonraki " +
                                "ekranda bu izni verebilirsiniz")
                        .setIcon(R.drawable.access)
                        .setCancelable(false)
                        .setPositiveButton("Anladım", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                            }
                        }).create();
                dialog.show();

                return false;
            }
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(Anasayfa.this, "Dosyalara erişemeden uygulama malesef çalışamaz", Toast.LENGTH_SHORT).show();
                    finish();
                }

        }
    }

    void mp3_indir(String url, String name) {
        if (request != null) {
            request = null;
        }
        request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle(name);
        request.setDescription("Şarkınız indiriliyor");
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name + ".mp3");
        if (InternetVarMi(Anasayfa.this)) {
            manager.enqueue(request);
        } else {
            Toast.makeText(Anasayfa.this, "İnternet bağlantınız yok", Toast.LENGTH_SHORT).show();
        }
    }

    void reklamSaydir() {
        Dosyalar.counterReklam++;
        Log.e("counterReklam", String.valueOf(Dosyalar.counterReklam));
        if (Dosyalar.counterReklam >= 4) {
            if (mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
            } else {
                requestNewInterstitial();
                if (mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                }
            }
        }

    }


    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("9874BE888797A663383EBCBD585E270A")
                .build();
        mInterstitialAd.loadAd(adRequest);
    }

    Dialog sDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anasayfa);
        preferences = getSharedPreferences("dk", MODE_PRIVATE);
        if (!InternetVarMi(Anasayfa.this)) {
            finish();
        }

        final AdView adView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("9874BE888797A663383EBCBD585E270A")
                .build();
        adView.loadAd(adRequest);
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getResources().getString(R.string.tamekranreklam));
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                requestNewInterstitial();
                Log.w("Reklam", " kapatıldı");
                Dosyalar.counterReklam = 0;
            }
        });
        requestNewInterstitial();

        yazilariYaz();

        turkHandler.removeCallbacksAndMessages(UpdateSongTime);

        isYazmaIzni(Anasayfa.this);
        relativMediaPlayer = (RelativeLayout) findViewById(R.id.relativeMediaPlayer);
        mPlayer = new MediaPlayer();

        manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);


        relativMediaPlayer.setVisibility(View.GONE);
        if (mPlayer.isPlaying()) {
            relativMediaPlayer.setVisibility(View.VISIBLE);
        }


        listKategoriler.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                reklamSaydir();

                if (InternetVarMi(Anasayfa.this)) {
                    new listeyiGetir().execute(Dosyalar.kategoriLinkleri.get(position));
                } else {
                    Toast.makeText(Anasayfa.this, "İnternet bağlantınız yok", Toast.LENGTH_SHORT).show();
                }
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                }
            }
        });

        imageSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String aranacak = editAranacak.getText().toString();
                reklamSaydir();
                if (aranacak.length() > 3) {
                    new aramaListeyiGetir().execute(aranacak);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    editAranacak.setText("");
                } else {
                    Toast.makeText(Anasayfa.this, "Aranacak kelime en az 3 harf olmalı", Toast.LENGTH_SHORT).show();
                }
            }
        });

        listSong.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                reklamSaydir();

                final Song song = Dosyalar.songs.get(position);
                currentListenUrl = song.listenLink;
                sDialog = new Dialog(Anasayfa.this);
                sDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                sDialog.setContentView(R.layout.dialog_secim);
                Button btnOynat = (Button) sDialog.findViewById(R.id.btnDialogDinle);
                Button btnIndir = (Button) sDialog.findViewById(R.id.btnDialogIndir);
                TextView textSarkiAdi = (TextView) sDialog.findViewById(R.id.textDialogSarkiAdi);
                textSarkiAdi.setText(song.name.trim());
                btnOynat.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        textSanatci_SarkiAdi.setText(song.author + "-" + song.name);
                        int dakika = Integer.parseInt(song.time.substring(0, 2));
                        int saniye = Integer.parseInt(song.time.substring(3, 5));
                        final int sureSaniye = dakika * 60 + saniye;
                        if (mPlayer != null) {
                            if (mPlayer.isPlaying()) {
                                mPlayer.stop();
                                mPlayer.setOnBufferingUpdateListener(null);
                                seekBar.setSecondaryProgress(0);
                                textBuffer.setText("Hafızaya alınıyor..%0");
                                mPlayer = null;
                                imagePlayPause.setImageResource(R.drawable.play);
                                imageSound.setImageResource(R.drawable.soundon);
                            }
                        }
                        mPlayer = new MediaPlayer();
                        try {
                            mPlayer.setDataSource(Anasayfa.this, Uri.parse(currentListenUrl));
                            imagePlayPause.setImageResource(R.drawable.play);
                            seekBar.setMax(sureSaniye);

                            turkHandler.postDelayed(UpdateSongTime, 1000);
                            mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                @Override
                                public void onPrepared(final MediaPlayer mp) {
                                    mp.start();
                                    imagePlayPause.setImageResource(R.drawable.pause);

                                }
                            });
                            mPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                                @Override
                                public void onBufferingUpdate(MediaPlayer mp, int percent) {
                                    if (percent != 100) {
                                        textBuffer.setText("Hafızaya alınıyor..%" + String.valueOf(percent));
                                        seekBar.setSecondaryProgress((percent * sureSaniye) / 100);
                                    } else if (percent == 0) {
                                        textBuffer.setText("Hafızaya alınıyor.. %0");
                                        seekBar.setSecondaryProgress(0);
                                    } else {
                                        textBuffer.setText("Hafızaya alındı. %100");
                                        seekBar.setSecondaryProgress(seekBar.getMax());
                                    }
                                }
                            });
                            relativMediaPlayer.setVisibility(View.VISIBLE);
                            mPlayer.prepareAsync();


                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        sDialog.dismiss();
                    }

                });
                btnIndir.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mp3_indir(song.downloadLink, song.name);
                        sDialog.dismiss();
                    }
                });
                sDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAdem;
                sDialog.show();


            }
        });

        imagePlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayer.isPlaying()) {
                    mPlayer.pause();
                    imagePlayPause.setImageResource(R.drawable.play);
                } else {
                    mPlayer.start();
                    imagePlayPause.setImageResource(R.drawable.pause);
                }
            }
        });
        imageStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayer.isPlaying()) {
                    mPlayer.stop();
                    mPlayer.release();
                    mPlayer = null;
                    imagePlayPause.setImageResource(R.drawable.play);
                }
                relativMediaPlayer.setVisibility(View.GONE);
                turkHandler.removeCallbacksAndMessages(UpdateSongTime);
            }
        });

        imageSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sesVarmi) {
                    mPlayer.setVolume(0, 0);
                    imageSound.setImageResource(R.drawable.soundoff);
                    sesVarmi = false;
                } else {
                    mPlayer.setVolume(1, 1);
                    imageSound.setImageResource(R.drawable.soundon);
                    sesVarmi = true;
                }

            }
        });


        if (InternetVarMi(Anasayfa.this)) {
            new listeyiGetir().execute("http://mp3-pm.info/");
        }
        new kategorileriGetir().execute();

    }

    boolean sesVarmi = true;

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.anasayfa, menu);
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            AlertDialog fDialog = new AlertDialog.Builder(this)
                    .setTitle("Çıkış")
                    .setMessage("Çıkış yapmak istediğinizden emin misiniz?")
                    .setIcon(R.drawable.exiticon)
                    .setPositiveButton("Evet", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton("Vazgeç", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .create();
            fDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAdem;
            fDialog.show();

        } else {
            return true;
        }
        return false;
    }

    public boolean internetVarMi() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager.getActiveNetworkInfo() != null && manager.getActiveNetworkInfo().isAvailable()
                && manager.getActiveNetworkInfo().isConnected()) {
            return true;

        } else
            return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.ac_paylas:
                paylas();
                break;
            case R.id.ac_diger_uygulamalar:
                Uri uri = Uri.parse("http://dkadem.com"); // missing 'http://' will cause crashed
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                break;
            case R.id.ac_cikis:
                cikisYap();
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    AlertDialog aDialog;

    void cikisYap() {
        aDialog = new AlertDialog.Builder(this)
                .setTitle("Onay")
                .setMessage("Çıkmak istiyor musunuz?")
                .setIcon(R.drawable.exiticon)
                .setPositiveButton("Çıkış", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton("Vazgeç", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).create();
        aDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAdem;
        aDialog.show();
    }

    void paylas() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Ücretsiz mp3 indir  " + paylasLink);
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }


    class aramaListeyiGetir extends AsyncTask<String, String, String> {
        boolean baglanti = false;
        ProgressDialog pDialog;
        String sunucUrl = "http://mp3-pm.info/s/f/";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(Anasayfa.this);
            pDialog.setTitle("Bekleniyor..");
            pDialog.setMessage("Liste hazırlanıyor");
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (baglanti) {
                if (Dosyalar.songs.size() > 0) {
                    SongAdapter adapter = new SongAdapter(Anasayfa.this, Dosyalar.songs);
                    listSong.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                } else {
                    listSong.setAdapter(null);
                    Toast.makeText(Anasayfa.this, "Malesef hiç şarkı bulunamadı", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(Anasayfa.this, "Bağlantıda bir problem meydana geldi", Toast.LENGTH_SHORT).show();
            }
            if (pDialog.isShowing())
                pDialog.dismiss();

        }

        @Override
        protected String doInBackground(String... params) {

            String kelime = params[0];
            try {
                Document doc = Jsoup.connect(sunucUrl + kelime).timeout(10000).ignoreHttpErrors(true).userAgent("Chrome/41.0.2228.0").get();
                Elements names = doc.select("b.cplayer-data-sound-title");
                Elements authors = doc.select("i.cplayer-data-sound-author");
                Elements times = doc.select("em.cplayer-data-sound-time");
                Elements links = doc.select("li.cplayer-sound-item");
                int tNames = names.size();
                int tAuthors = authors.size();
                int tTimes = times.size();
                int tLinks = links.size();
                Dosyalar.songs.clear();
                if (tNames == tAuthors && tTimes == tLinks) {
                    for (int i = 0; i < tNames; i++) {
                        Song song = new Song();
                        song.author = authors.get(i).text();
                        song.name = names.get(i).text();
                        song.time = times.get(i).text();
                        song.listenLink = links.get(i).attr("data-sound-url");
                        song.downloadLink = links.get(i).attr("data-download-url");
                        Dosyalar.songs.add(song);
                    }
                }
                baglanti = true;
            } catch (IOException e) {
                e.printStackTrace();
            }


            //div.mp3list-btns
            return null;
        }
    }

    class listeyiGetir extends AsyncTask<String, String, String> {
        boolean baglanti = false;
        ProgressDialog pDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(Anasayfa.this);
            pDialog.setTitle("Bekleniyor..");
            pDialog.setMessage("Liste hazırlanıyor");
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (baglanti) {
                if (Dosyalar.songs.size() > 0) {
                    SongAdapter adapter = new SongAdapter(Anasayfa.this, Dosyalar.songs);
                    listSong.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(Anasayfa.this, "Veriler gelirken bir hata meydana geldi", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(Anasayfa.this, "Bağlantıda bir problem meydana geldi", Toast.LENGTH_SHORT).show();
            }
            if (pDialog.isShowing())
                pDialog.dismiss();

        }

        @Override
        protected String doInBackground(String... params) {

            String adres = params[0];
            try {
                Document doc = Jsoup.connect(adres).userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.1").get();
                Elements names = doc.select("b.cplayer-data-sound-title");
                Elements authors = doc.select("i.cplayer-data-sound-author");
                Elements times = doc.select("em.cplayer-data-sound-time");
                Elements links = doc.select("li.cplayer-sound-item");
                int tNames = names.size();
                int tAuthors = authors.size();
                int tTimes = times.size();
                int tLinks = links.size();
                Dosyalar.songs.clear();
                if (tNames == tAuthors && tTimes == tLinks) {
                    for (int i = 0; i < tNames; i++) {
                        Song song = new Song();
                        song.author = authors.get(i).text();
                        song.name = names.get(i).text();
                        song.time = times.get(i).text();
                        song.listenLink = links.get(i).attr("data-sound-url");
                        song.downloadLink = links.get(i).attr("data-download-url");
                        Dosyalar.songs.add(song);
                    }
                }
                baglanti = true;
            } catch (IOException e) {
                e.printStackTrace();
            }


            //div.mp3list-btns
            return null;
        }
    }

    void duyuruGoster(String text, final String duyuruLink) {
        final AlertDialog dDialog = new AlertDialog.Builder(Anasayfa.this)
                .setTitle("Duyuru")
                .setMessage(text)
                .setIcon(R.drawable.notice)
                .setPositiveButton("Tamam", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!duyuruLink.equals("")) {
                            Uri uri = Uri.parse(duyuruLink); // missing 'http://' will cause crashed
                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            startActivity(intent);
                        }
                    }
                })
                .create();
        dDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAdem;
        dDialog.show();
    }


    class kategorileriGetir extends AsyncTask<Void, Void, Void> {

        boolean baglanti = false;


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (baglanti) {
                if (Dosyalar.kategoriAdlari.size() > 0 && Dosyalar.kategoriLinkleri.size() > 0) {
                    ArrayAdapter arrayAdapter = new ArrayAdapter(Anasayfa.this, android.R.layout.simple_list_item_1, Dosyalar.kategoriAdlari);
                    arrayAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
                    listKategoriler.setAdapter(arrayAdapter);
                }
            }

        }

        @Override
        protected Void doInBackground(Void... params) {


            try {

                Document doc = Jsoup.connect("http://dkadem.net/mobilapps/mp3kategoriler.php").data("konu", "konu").post();
                String html = doc.text();
                JSONArray parcala = new JSONArray(html);
                if (parcala.length() > 0) {
                    for (int i = 0; i < parcala.length(); i++) {
                        JSONObject obj = parcala.getJSONObject(i);
                        Dosyalar.kategoriAdlari.add(obj.getString("bilgi"));
                        Dosyalar.kategoriLinkleri.add(obj.getString("link"));
                    }
                }
                baglanti = true;
            } catch (JSONException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        return null;
    }
}
}
