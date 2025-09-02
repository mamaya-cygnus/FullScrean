package com.full.screan.ui.home;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.full.screan.R;
import com.full.screan.databinding.FragmentHomeBinding;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private WebView webView;
    private SwipeRefreshLayout swipeRefreshLayout;
//    private static final String JSON_SERVER_URL = "http://10.20.0.220/url.json";
    private static final String JSON_SERVER_URL = "https://cygnusinv.com/url.json";
    private static final String LOCAL_JSON_FILE = "config.json";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        webView = binding.webView;

        swipeRefreshLayout = root.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            reloadWebViewLocation();
        });

        // Configuración del WebView mejorada
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);

        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.requestFocus(View.FOCUS_DOWN);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Oculta la animación de recarga
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimetype);

            // Cabecera para que respete cookies y user-agent
            String cookies = CookieManager.getInstance().getCookie(url);
            if (cookies != null) {
                request.addRequestHeader("Cookie", cookies);
            }
            request.addRequestHeader("User-Agent", userAgent);

            // Nombre sugerido
            String filename = URLUtil.guessFileName(url, contentDisposition, mimetype);
            request.setTitle(filename);
            request.setDescription("Descargando archivo...");

            // Permitir ver la descarga en notificaciones
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            // Directorio de descargas
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);

            // Ejecutar descarga
            DownloadManager dm = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
            }

            Toast.makeText(getContext(), "Descargando: " + filename, Toast.LENGTH_LONG).show();
        });


        // Configurar ajuste automático para el teclado
        webView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && getActivity() != null) {
                getActivity().getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                );
            }
        });

        // Cargar URL desde JSON del servidor
        loadUrlFromJsonServer();

        setupWindowInsets(root);

        return root;
    }

    private void loadUrlFromJsonServer() {
        new Thread(() -> {
            String urlToLoad = null;

            try {
                // Intentar descargar JSON del servidor
                urlToLoad = downloadAndParseJson();

                if (urlToLoad != null) {
                    // Si se descargó exitosamente, guardarlo localmente
                    saveJsonLocally(urlToLoad);
                }
            } catch (Exception e) {
                Log.e("HomeFragment", "Error downloading from server", e);
            }

            // Si no se pudo descargar, usar el archivo local
            if (urlToLoad == null) {
                urlToLoad = loadFromLocalJson();
            }

            // Si aún no hay URL, usar una por defecto
            if (urlToLoad == null) {
                urlToLoad = "https://www.google.com";
            }

            // Asegurar que tenga protocolo
            urlToLoad = ensureProtocol(urlToLoad);

            // Cargar en el WebView
            String finalUrl = urlToLoad;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Log.d("HomeFragment", "Loading URL: " + finalUrl);
                    webView.loadUrl(finalUrl);
                });
            }
        }).start();
    }

    private String downloadAndParseJson() throws Exception {
        URL url = new URL(JSON_SERVER_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.addRequestProperty("Cache-Control", "no-cache");

        if (connection.getResponseCode() == 200 || connection.getResponseCode() == 304) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            reader.close();
            connection.disconnect();

            // Parsear JSON
            JSONObject jsonObject = new JSONObject(result.toString());
            Log.d("HomeFragment", "Respuesta JSON: " + result.toString());

            return jsonObject.getString("url"); // Asume que el JSON tiene: {"url": "10.20.0.220/erp/public/"}
        }

        throw new Exception("Server responded with code: " + connection.getResponseCode());
    }

    private void saveJsonLocally(String url) {
        try {
            // Crear JSON local
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("url", url);
            jsonObject.put("lastUpdated", System.currentTimeMillis());

            // Guardar en archivo interno
            FileOutputStream fos = requireContext().openFileOutput(LOCAL_JSON_FILE, Context.MODE_PRIVATE);
            fos.write(jsonObject.toString().getBytes());
            fos.close();

            Log.d("HomeFragment", "JSON saved locally");
        } catch (Exception e) {
            Log.e("HomeFragment", "Error saving JSON locally", e);
        }
    }

    private String loadFromLocalJson() {
        try {
            FileInputStream fis = requireContext().openFileInput(LOCAL_JSON_FILE);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader reader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            fis.close();

            JSONObject jsonObject = new JSONObject(sb.toString());
            String url = jsonObject.getString("url");

            Log.d("HomeFragment", "Loaded from local JSON: " + url);
            return url;

        } catch (Exception e) {
            Log.e("HomeFragment", "Error loading local JSON", e);
            return null;
        }
    }

    private String ensureProtocol(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            // Para IPs locales, usar HTTP
            if (url.startsWith("10.") || url.startsWith("192.168.") ||
                    url.startsWith("172.") || url.startsWith("localhost")) {
                return "http://" + url;
            } else {
                return "https://" + url;
            }
        }
        return url;
    }

    private void setupWindowInsets(View root) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                // Permitir ajuste para el teclado
                return insets;
            });
        } else {
            root.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                return insets.consumeSystemWindowInsets();
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Cambia tu método reloadWebView()
    public void reloadWebViewLocation() {
        if (webView != null) {
            webView.reload();
        }
    }

    public void reloadWebView() {
        if (webView != null) {
            loadUrlFromJsonServer(); // Siempre intenta descargar desde servidor
        }
    }
    public boolean canGoBack() {
        return webView != null && webView.canGoBack();
    }

    // Método para retroceder en el WebView
    public void goBack() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        }
    }
}