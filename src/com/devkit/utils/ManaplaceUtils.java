package com.devkit.utils;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.MediaUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@DesignerComponent(
        version = 1,
        description = "Extension ManaplaceUtils - Barre de navigation flottante, Chat, Produits, Catégories, Serveur, Médias et Effets Visuels.",
        category = ComponentCategory.EXTENSION,
        nonVisible = true
)
@SimpleObject(external = true)
public class ManaplaceUtils extends AndroidNonvisibleComponent implements ActivityResultListener {

    private final Context context;
    private final Activity activity;
    private final Form monForm;
    private Dialog activeAlphaDialog;
    private final int PICK_IMAGE_REQUEST = 1001;

    // --- État de la barre de navigation flottante ---
    private boolean dejaInitialise = false;
    private int tailleIconeDp = 26;
    private final List<String> idsEnAttente = new ArrayList<>();
    private final List<String> iconesEnAttente = new ArrayList<>();
    private final List<ImageView> vuesIcones = new ArrayList<>();
    private final List<View> vuesCercles = new ArrayList<>();
    private final List<String> idsFinaux = new ArrayList<>();
    private String idSelectionne = null;

    public ManaplaceUtils(ComponentContainer container) {
        super(container.$form());
        this.context = container.$context();
        this.activity = (Activity) container.$context();
        this.monForm = container.$form();
        this.form.registerForActivityResult(this);
    }

    // =========================================================================
    // 0. BARRE DE NAVIGATION FLOTTANTE
    // =========================================================================

    @SimpleFunction(description = "Ajoute une icône à la barre de navigation. À appeler une fois par icône, avant NavBarInitialize.")
    public void NavBarAdd(String id, String icon) {
        if (idsEnAttente.contains(id)) {
            NavBarError("Id déjà utilisé: " + id);
            return;
        }
        idsEnAttente.add(id);
        iconesEnAttente.add(icon);
    }

    @SimpleFunction(description = "Construit et affiche la barre flottante avec toutes les icônes ajoutées via NavBarAdd.")
    public void NavBarInitialize(int margeBas, double largeurPourcent, double hauteurPourcent) {
        if (dejaInitialise) return;

        if (idsEnAttente.isEmpty()) {
            NavBarError("Aucune icône ajoutée — appelle NavBarAdd avant NavBarInitialize");
            return;
        }

        try {
            FrameLayout root = (FrameLayout) activity.findViewById(android.R.id.content);
            if (root == null) {
                NavBarError("Écran racine introuvable");
                return;
            }

            DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
            int largeurFinale = (largeurPourcent > 0) ? (int) (metrics.widthPixels * (largeurPourcent / 100.0)) : ViewGroup.LayoutParams.WRAP_CONTENT;
            int hauteurFinale = (hauteurPourcent > 0) ? (int) (metrics.heightPixels * (hauteurPourcent / 100.0)) : (int) dpToPx(64);

            LinearLayout bar = new LinearLayout(activity);
            bar.setOrientation(LinearLayout.HORIZONTAL);
            bar.setGravity(Gravity.CENTER);
            bar.setWeightSum(idsEnAttente.size());

            GradientDrawable fond = new GradientDrawable();
            fond.setColor(Color.WHITE);
            fond.setCornerRadius(dpToPx(30));
            bar.setBackground(fond);
            bar.setElevation(dpToPx(12));

            for (int i = 0; i < idsEnAttente.size(); i++) {
                final String tabId = idsEnAttente.get(i);
                String iconFile = iconesEnAttente.get(i);

                FrameLayout conteneur = new FrameLayout(activity);
                LinearLayout.LayoutParams pConteneur = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
                conteneur.setLayoutParams(pConteneur);

                View cercle = new View(activity);
                GradientDrawable fondCercle = new GradientDrawable();
                fondCercle.setShape(GradientDrawable.OVAL);
                fondCercle.setColor(Color.argb(30, 0, 0, 0));
                cercle.setBackground(fondCercle);
                cercle.setAlpha(0f);
                FrameLayout.LayoutParams pCercle = new FrameLayout.LayoutParams((int) dpToPx(46), (int) dpToPx(46), Gravity.CENTER);
                conteneur.addView(cercle, pCercle);

                ImageView img = new ImageView(activity);
                img.setAdjustViewBounds(true);
                try {
                    Drawable d = MediaUtil.getBitmapDrawable(monForm, iconFile);
                    img.setImageDrawable(d);
                    img.setColorFilter(new PorterDuffColorFilter(Color.rgb(150, 150, 150), PorterDuff.Mode.SRC_IN));
                } catch (IOException e) {
                    NavBarError("Icône introuvable: " + iconFile);
                }
                int taillePx = (int) dpToPx(tailleIconeDp);
                FrameLayout.LayoutParams pImg = new FrameLayout.LayoutParams(taillePx, taillePx, Gravity.CENTER);
                conteneur.addView(img, pImg);

                final View cercleFinal = cercle;
                final ImageView imgFinal = img;
                conteneur.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SelectionnerOnglet(tabId, cercleFinal, imgFinal);
                    }
                });

                vuesIcones.add(img);
                vuesCercles.add(cercle);
                idsFinaux.add(tabId);
                bar.addView(conteneur);
            }

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(largeurFinale, hauteurFinale);
            params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            params.setMargins(0, 0, 0, (int) dpToPx(margeBas));

            root.addView(bar, params);
            dejaInitialise = true;

        } catch (Exception e) {
            NavBarError("Erreur inattendue: " + e.getMessage());
        }
    }

    @SimpleFunction(description = "Ajuste la taille de toutes les icônes de la barre en dp, avec une transition animée.")
    public void NavBarSetIconSize(final int tailleDp) {
        final int ancienneTailleDp = tailleIconeDp;
        tailleIconeDp = tailleDp;

        if (vuesIcones.isEmpty()) return;

        try {
            final float ancienPx = dpToPx(ancienneTailleDp);
            final float nouveauPx = dpToPx(tailleDp);

            ValueAnimator anim = ValueAnimator.ofFloat(ancienPx, nouveauPx);
            anim.setDuration(220);
            anim.setInterpolator(new DecelerateInterpolator());
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int taillePx = (int) (float) animation.getAnimatedValue();
                    for (ImageView iv : vuesIcones) {
                        ViewGroup.LayoutParams p = iv.getLayoutParams();
                        p.width = taillePx;
                        p.height = taillePx;
                        iv.setLayoutParams(p);
                    }
                }
            });
            anim.start();

        } catch (Exception e) {
            NavBarError("Erreur NavBarSetIconSize: " + e.getMessage());
        }
    }

    @SimpleFunction(description = "Sélectionne un onglet de la barre par code, sans clic.")
    public void NavBarSelect(String id) {
        int index = idsFinaux.indexOf(id);
        if (index < 0) {
            NavBarError("Id introuvable pour NavBarSelect: " + id);
            return;
        }
        SelectionnerOnglet(id, vuesCercles.get(index), vuesIcones.get(index));
    }

    private void SelectionnerOnglet(String id, View cercle, ImageView img) {
        try {
            if (id.equals(idSelectionne)) return;

            if (idSelectionne != null) {
                int ancienIndex = idsFinaux.indexOf(idSelectionne);
                if (ancienIndex >= 0) {
                    animerOnglet(vuesCercles.get(ancienIndex), vuesIcones.get(ancienIndex), false);
                }
            }

            animerOnglet(cercle, img, true);
            idSelectionne = id;
            OnSelected(id);

        } catch (Exception e) {
            NavBarError("Erreur de sélection: " + e.getMessage());
        }
    }

    private void animerOnglet(final View cercle, final ImageView img, boolean selectionne) {
        float alphaCible = selectionne ? 1f : 0f;
        ValueAnimator anim = ValueAnimator.ofFloat(cercle.getAlpha(), alphaCible);
        anim.setDuration(220);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                cercle.setAlpha(val);
                int couleur = melangerCouleurs(Color.rgb(150, 150, 150), Color.rgb(20, 20, 20), val);
                img.setColorFilter(new PorterDuffColorFilter(couleur, PorterDuff.Mode.SRC_IN));
            }
        });
        anim.start();
    }

    private int melangerCouleurs(int c1, int c2, float ratio) {
        int r = (int) (Color.red(c1) + ratio * (Color.red(c2) - Color.red(c1)));
        int g = (int) (Color.green(c1) + ratio * (Color.green(c2) - Color.green(c1)));
        int b = (int) (Color.blue(c1) + ratio * (Color.blue(c2) - Color.blue(c1)));
        return Color.rgb(r, g, b);
    }

    private float dpToPx(int dp) {
        float density = activity.getResources().getDisplayMetrics().density;
        return dp * density;
    }

    // =========================================================================
    // 1. MOTEUR DE CHAT & DÉFILEMENT AUTOMATIQUE
    // =========================================================================

    @SimpleFunction(description = "Ajoute une bulle de message dynamique en utilisant un Template Kodular.")
    public void AddChatMessageFromTemplate(
            final AndroidViewComponent chatContainer,
            final AndroidViewComponent templateBubbleCard,
            final String messageText,
            final String timestamp,
            final String senderUid,
            final boolean isMe,
            final int bubbleColor,
            final String avatarUrl) {

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    View containerView = chatContainer.getView();
                    View templateView = templateBubbleCard.getView();

                    if (containerView instanceof ViewGroup) {
                        ViewGroup parentLayout = (ViewGroup) containerView;

                        templateView.setBackgroundColor(bubbleColor);

                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        params.gravity = isMe ? Gravity.END : Gravity.START;
                        params.setMargins(12, 8, 12, 8);
                        templateView.setLayoutParams(params);

                        templateView.setTag(senderUid);
                        templateView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                OnUserAvatarClick(senderUid, isMe ? "me" : "user");
                            }
                        });

                        parentLayout.addView(templateView);
                        ScrollToBottom(chatContainer);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @SimpleFunction(description = "Fait défiler le ScrollArrangement jusqu'au tout dernier message.")
    public void ScrollToBottom(final AndroidViewComponent scrollContainer) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View view = scrollContainer.getView();
                if (view instanceof ScrollView) {
                    final ScrollView scrollView = (ScrollView) view;
                    scrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollView.fullScroll(View.FOCUS_DOWN);
                        }
                    });
                }
            }
        });
    }

    // =========================================================================
    // 2. SAISIE FLOTTANTE & CLAVIER
    // =========================================================================

    @SimpleFunction(description = "Attache la zone de saisie au-dessus du clavier.")
    public void AttachFloatingInputWithDynamicHeight(
            final AndroidViewComponent inputContainer,
            final AndroidViewComponent editTextComponent,
            final int maxHeightPx) {

        final View containerView = inputContainer.getView();
        final View rootView = activity.getWindow().getDecorView().getRootView();

        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                android.graphics.Rect r = new android.graphics.Rect();
                rootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootView.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                if (keypadHeight > screenHeight * 0.15) {
                    containerView.setTranslationY(-keypadHeight);
                } else {
                    containerView.setTranslationY(0);
                }
            }
        });
    }

    // =========================================================================
    // 3. CATALOGUE DE PRODUITS
    // =========================================================================

    @SimpleFunction(description = "Construit la grille de produits depuis un JSON.")
    public void BuildProductGridFromJson(
            final AndroidViewComponent scrollContainer,
            final AndroidViewComponent templateCard,
            final String jsonData) {

        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                try {
                    final JSONArray jsonArray = new JSONArray(jsonData);

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                View containerView = scrollContainer.getView();
                                if (containerView instanceof ViewGroup) {
                                    ViewGroup layout = (ViewGroup) containerView;

                                    for (int i = 0; i < jsonArray.length(); i++) {
                                        JSONObject item = jsonArray.getJSONObject(i);
                                        final String productUid = item.optString("uid", "");

                                        View cardClone = templateCard.getView();
                                        cardClone.setTag(productUid);
                                        cardClone.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                OnProductCardClick(productUid);
                                            }
                                        });

                                        layout.addView(cardClone);
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // =========================================================================
    // 3B. LISTE DYNAMIQUE DE CATÉGORIES (construite nativement, sans clonage)
    // =========================================================================

    @SimpleFunction(description = "Génère la liste des catégories/sous-catégories depuis un JSON. sectionColor/itemColor: couleurs du texte.")
    public void BuildCategoryListFromJson(
            final AndroidViewComponent listContainer,
            final int sectionColor,
            final int itemColor,
            final String categoriesJson) {

        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                try {
                    final JSONArray mainArray = new JSONArray(categoriesJson);

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                View containerView = listContainer.getView();
                                if (!(containerView instanceof ViewGroup)) return;

                                ViewGroup layout = (ViewGroup) containerView;
                                layout.removeAllViews();

                                RadioGroup groupeUnique = new RadioGroup(activity);
                                groupeUnique.setOrientation(LinearLayout.VERTICAL);

                                for (int i = 0; i < mainArray.length(); i++) {
                                    JSONObject categoryObj = mainArray.getJSONObject(i);
                                    String categoryName = categoryObj.optString("title", "");
                                    JSONArray subCategories = categoryObj.optJSONArray("subcategories");

                                    TextView header = new TextView(activity);
                                    header.setText("›  " + categoryName);
                                    header.setTextColor(sectionColor);
                                    header.setTextSize(18);
                                    header.setTypeface(header.getTypeface(), Typeface.BOLD);
                                    int padPx = (int) dpToPx(12);
                                    header.setPadding(padPx, padPx, padPx, (int) dpToPx(6));
                                    layout.addView(header);

                                    if (subCategories != null) {
                                        for (int j = 0; j < subCategories.length(); j++) {
                                            JSONObject subObj = subCategories.getJSONObject(j);
                                            final String subId = subObj.optString("id", "");
                                            final String subTitle = subObj.optString("title", "");

                                            RadioButton bouton = new RadioButton(activity);
                                            bouton.setText(subTitle);
                                            bouton.setTextColor(itemColor);
                                            bouton.setTextSize(16);
                                            bouton.setPadding((int) dpToPx(16), (int) dpToPx(10), (int) dpToPx(16), (int) dpToPx(10));
                                            bouton.setTag(subId);

                                            bouton.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    OnCategorySelected(subId, subTitle);
                                                }
                                            });

                                            groupeUnique.addView(bouton);
                                        }
                                    }
                                }

                                layout.addView(groupeUnique);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // =========================================================================
    // 4. EFFETS VISUELS (DÉGRADÉ & FLOU GLASSMORPHISM)
    // =========================================================================

    @SimpleFunction(description = "Applique un dégradé de couleur sur un composant.")
    public void SetGradientBackground(
            final AndroidViewComponent component,
            final int startColor,
            final int endColor,
            final String orientation) {

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    GradientDrawable.Orientation gradOrientation = GradientDrawable.Orientation.TOP_BOTTOM;
                    if ("LEFT_RIGHT".equalsIgnoreCase(orientation)) {
                        gradOrientation = GradientDrawable.Orientation.LEFT_RIGHT;
                    }

                    GradientDrawable gd = new GradientDrawable(
                            gradOrientation,
                            new int[]{startColor, endColor}
                    );
                    gd.setCornerRadius(0f);
                    component.getView().setBackground(gd);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @SimpleFunction(description = "Applique un effet de flou (Glassmorphism) sur un composant.")
    public void SetBlurEffect(final AndroidViewComponent component, final float radius) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    View view = component.getView();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        float blurRadius = Math.max(1f, Math.min(radius, 25f));
                        view.setRenderEffect(RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP));
                    } else {
                        view.setBackgroundColor(Color.argb(150, 255, 255, 255));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // =========================================================================
    // 5. DIALOGUE TRANSPARENT, NOTIFICATEUR & GESTION SONORE
    // =========================================================================

    @SimpleFunction(description = "Affiche un composant sous forme de dialogue transparent.")
    public void ShowAlphaDialog(final AndroidViewComponent dialogContentLayout, final boolean cancelable) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (activeAlphaDialog != null && activeAlphaDialog.isShowing()) {
                        activeAlphaDialog.dismiss();
                    }

                    activeAlphaDialog = new Dialog(activity);
                    activeAlphaDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

                    View contentView = dialogContentLayout.getView();
                    if (contentView.getParent() != null) {
                        ((ViewGroup) contentView.getParent()).removeView(contentView);
                    }

                    activeAlphaDialog.setContentView(contentView);

                    if (activeAlphaDialog.getWindow() != null) {
                        activeAlphaDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                        activeAlphaDialog.getWindow().setLayout(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        );
                    }

                    activeAlphaDialog.setCancelable(cancelable);
                    activeAlphaDialog.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @SimpleFunction(description = "Ferme le dialogue Alpha.")
    public void DismissAlphaDialog() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (activeAlphaDialog != null && activeAlphaDialog.isShowing()) {
                    activeAlphaDialog.dismiss();
                    activeAlphaDialog = null;
                }
            }
        });
    }

    @SimpleFunction(description = "Notification personnalisée temporaire.")
    public void CustomNotifier(final AndroidViewComponent customLayout, final int durationMs) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ShowAlphaDialog(customLayout, true);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        DismissAlphaDialog();
                    }
                }, durationMs);
            }
        });
    }

    @SimpleFunction(description = "Joue un son personnalisé (ex: envoi de message, notification).")
    public void PlayCustomSound(final String fileNameOrPath) {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                MediaPlayer mediaPlayer = null;
                try {
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setAudioAttributes(
                            new AudioAttributes.Builder()
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                                    .build()
                    );

                    if (fileNameOrPath.startsWith("/")) {
                        mediaPlayer.setDataSource(fileNameOrPath);
                    } else {
                        android.content.res.AssetFileDescriptor afd = context.getAssets().openFd(fileNameOrPath);
                        mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        afd.close();
                    }

                    mediaPlayer.prepare();
                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            mp.release();
                        }
                    });
                    mediaPlayer.start();
                } catch (Exception e) {
                    e.printStackTrace();
                    if (mediaPlayer != null) {
                        mediaPlayer.release();
                    }
                }
            }
        });
    }

    // =========================================================================
    // 6. GALERIE D'IMAGES & COMPRESSION
    // =========================================================================

    @SimpleFunction(description = "Ouvre la galerie d'images native.")
    public void OpenPhotoPicker() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.setType("image/*");
                    activity.startActivityForResult(intent, PICK_IMAGE_REQUEST);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void resultReturned(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                OnPhotoPicked(selectedImageUri.toString());
            }
        }
    }

    @SimpleFunction(description = "Compresse une image sans surcharger la mémoire.")
    public String CompressImage(String imagePath, int quality, int maxWidth) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imagePath, options);

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return imagePath;
            }

            int srcWidth = options.outWidth;
            int inSampleSize = 1;

            if (srcWidth > maxWidth) {
                inSampleSize = Math.round((float) srcWidth / (float) maxWidth);
            }

            options.inJustDecodeBounds = false;
            options.inSampleSize = inSampleSize;

            Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);
            if (bitmap == null) {
                return imagePath;
            }

            File outputFile = new File(context.getCacheDir(), "comp_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(outputFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
                out.flush();
            } finally {
                if (out != null) {
                    out.close();
                }
                bitmap.recycle();
            }

            return outputFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return imagePath;
        }
    }

    // =========================================================================
    // 7. REQUÊTES SERVEUR (ASYNCHRONE, sans dépendance externe)
    // =========================================================================

    @SimpleFunction(description = "Envoie une requête HTTPS au serveur.")
    public void CallServerRequest(final String endpointUrl, final String method, final String headersJson, final String bodyJson) {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(endpointUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);
                    conn.setRequestMethod("POST".equalsIgnoreCase(method) ? "POST" : "GET");

                    if (headersJson != null && !headersJson.isEmpty()) {
                        JSONObject headers = new JSONObject(headersJson);
                        Iterator<String> keys = headers.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            conn.setRequestProperty(key, headers.getString(key));
                        }
                    }

                    if ("POST".equalsIgnoreCase(method)) {
                        conn.setDoOutput(true);
                        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                        if (bodyJson != null) {
                            OutputStream os = conn.getOutputStream();
                            os.write(bodyJson.getBytes("UTF-8"));
                            os.flush();
                            os.close();
                        }
                    }

                    final int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 400) ? conn.getInputStream() : conn.getErrorStream();
                    final String responseContent = lireFlux(is);

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            OnServerResponse(responseCode, responseContent);
                        }
                    });

                } catch (final Exception e) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            OnServerResponse(500, e.getMessage());
                        }
                    });
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
        });
    }

    private String lireFlux(InputStream is) throws IOException {
        if (is == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String ligne;
        while ((ligne = reader.readLine()) != null) {
            sb.append(ligne);
        }
        reader.close();
        return sb.toString();
    }

    // =========================================================================
    // 8. ÉVÉNEMENTS (EVENTS) KODULAR
    // =========================================================================

    @SimpleEvent(description = "Déclenché quand l'utilisateur touche une icône de la barre de navigation.")
    public void OnSelected(String id) {
        EventDispatcher.dispatchEvent(this, "OnSelected", id);
    }

    @SimpleEvent(description = "Déclenché en cas de problème avec la barre de navigation.")
    public void NavBarError(String message) {
        EventDispatcher.dispatchEvent(this, "NavBarError", message);
    }

    @SimpleEvent(description = "Déclenché lors du clic sur un produit.")
    public void OnProductCardClick(String productUid) {
        EventDispatcher.dispatchEvent(this, "OnProductCardClick", productUid);
    }

    @SimpleEvent(description = "Déclenché lors du choix d'une catégorie. Renvoie l'ID et le Nom.")
    public void OnCategorySelected(String categoryId, String categoryTitle) {
        EventDispatcher.dispatchEvent(this, "OnCategorySelected", categoryId, categoryTitle);
    }

    @SimpleEvent(description = "Déclenché lors du clic sur l'avatar.")
    public void OnUserAvatarClick(String userUid, String userRole) {
        EventDispatcher.dispatchEvent(this, "OnUserAvatarClick", userUid, userRole);
    }

    @SimpleEvent(description = "Déclenché après sélection d'une image.")
    public void OnPhotoPicked(String imageUri) {
        EventDispatcher.dispatchEvent(this, "OnPhotoPicked", imageUri);
    }

    @SimpleEvent(description = "Déclenché après réponse du serveur.")
    public void OnServerResponse(int responseCode, String responseContent) {
        EventDispatcher.dispatchEvent(this, "OnServerResponse", responseCode, responseContent);
    }
}package com.devkit.utils;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.MediaUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@DesignerComponent(
        version = 1,
        description = "Extension ManaplaceUtils - Barre de navigation flottante, Chat, Produits, Catégories, Serveur, Médias et Effets Visuels.",
        category = ComponentCategory.EXTENSION,
        nonVisible = true
)
@SimpleObject(external = true)
public class ManaplaceUtils extends AndroidNonvisibleComponent implements ActivityResultListener {

    private final Context context;
    private final Activity activity;
    private final Form monForm;
    private Dialog activeAlphaDialog;
    private final int PICK_IMAGE_REQUEST = 1001;

    // --- État de la barre de navigation flottante ---
    private boolean dejaInitialise = false;
    private int tailleIconeDp = 26;
    private final List<String> idsEnAttente = new ArrayList<>();
    private final List<String> iconesEnAttente = new ArrayList<>();
    private final List<ImageView> vuesIcones = new ArrayList<>();
    private final List<View> vuesCercles = new ArrayList<>();
    private final List<String> idsFinaux = new ArrayList<>();
    private String idSelectionne = null;

    public ManaplaceUtils(ComponentContainer container) {
        super(container.$form());
        this.context = container.$context();
        this.activity = (Activity) container.$context();
        this.monForm = container.$form();
        this.form.registerForActivityResult(this);
    }

    // =========================================================================
    // 0. BARRE DE NAVIGATION FLOTTANTE
    // =========================================================================

    @SimpleFunction(description = "Ajoute une icône à la barre de navigation. À appeler une fois par icône, avant NavBarInitialize.")
    public void NavBarAdd(String id, String icon) {
        if (idsEnAttente.contains(id)) {
            NavBarError("Id déjà utilisé: " + id);
            return;
        }
        idsEnAttente.add(id);
        iconesEnAttente.add(icon);
    }

    @SimpleFunction(description = "Construit et affiche la barre flottante avec toutes les icônes ajoutées via NavBarAdd.")
    public void NavBarInitialize(int margeBas, double largeurPourcent, double hauteurPourcent) {
        if (dejaInitialise) return;

        if (idsEnAttente.isEmpty()) {
            NavBarError("Aucune icône ajoutée — appelle NavBarAdd avant NavBarInitialize");
            return;
        }

        try {
            FrameLayout root = (FrameLayout) activity.findViewById(android.R.id.content);
            if (root == null) {
                NavBarError("Écran racine introuvable");
                return;
            }

            DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
            int largeurFinale = (largeurPourcent > 0) ? (int) (metrics.widthPixels * (largeurPourcent / 100.0)) : ViewGroup.LayoutParams.WRAP_CONTENT;
            int hauteurFinale = (hauteurPourcent > 0) ? (int) (metrics.heightPixels * (hauteurPourcent / 100.0)) : (int) dpToPx(64);

            LinearLayout bar = new LinearLayout(activity);
            bar.setOrientation(LinearLayout.HORIZONTAL);
            bar.setGravity(Gravity.CENTER);
            bar.setWeightSum(idsEnAttente.size());

            GradientDrawable fond = new GradientDrawable();
            fond.setColor(Color.WHITE);
            fond.setCornerRadius(dpToPx(30));
            bar.setBackground(fond);
            bar.setElevation(dpToPx(12));

            for (int i = 0; i < idsEnAttente.size(); i++) {
                final String tabId = idsEnAttente.get(i);
                String iconFile = iconesEnAttente.get(i);

                FrameLayout conteneur = new FrameLayout(activity);
                LinearLayout.LayoutParams pConteneur = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
                conteneur.setLayoutParams(pConteneur);

                View cercle = new View(activity);
                GradientDrawable fondCercle = new GradientDrawable();
                fondCercle.setShape(GradientDrawable.OVAL);
                fondCercle.setColor(Color.argb(30, 0, 0, 0));
                cercle.setBackground(fondCercle);
                cercle.setAlpha(0f);
                FrameLayout.LayoutParams pCercle = new FrameLayout.LayoutParams((int) dpToPx(46), (int) dpToPx(46), Gravity.CENTER);
                conteneur.addView(cercle, pCercle);

                ImageView img = new ImageView(activity);
                img.setAdjustViewBounds(true);
                try {
                    Drawable d = MediaUtil.getBitmapDrawable(monForm, iconFile);
                    img.setImageDrawable(d);
                    img.setColorFilter(new PorterDuffColorFilter(Color.rgb(150, 150, 150), PorterDuff.Mode.SRC_IN));
                } catch (IOException e) {
                    NavBarError("Icône introuvable: " + iconFile);
                }
                int taillePx = (int) dpToPx(tailleIconeDp);
                FrameLayout.LayoutParams pImg = new FrameLayout.LayoutParams(taillePx, taillePx, Gravity.CENTER);
                conteneur.addView(img, pImg);

                final View cercleFinal = cercle;
                final ImageView imgFinal = img;
                conteneur.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SelectionnerOnglet(tabId, cercleFinal, imgFinal);
                    }
                });

                vuesIcones.add(img);
                vuesCercles.add(cercle);
                idsFinaux.add(tabId);
                bar.addView(conteneur);
            }

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(largeurFinale, hauteurFinale);
            params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            params.setMargins(0, 0, 0, (int) dpToPx(margeBas));

            root.addView(bar, params);
            dejaInitialise = true;

        } catch (Exception e) {
            NavBarError("Erreur inattendue: " + e.getMessage());
        }
    }

    @SimpleFunction(description = "Ajuste la taille de toutes les icônes de la barre en dp, avec une transition animée.")
    public void NavBarSetIconSize(final int tailleDp) {
        final int ancienneTailleDp = tailleIconeDp;
        tailleIconeDp = tailleDp;

        if (vuesIcones.isEmpty()) return;

        try {
            final float ancienPx = dpToPx(ancienneTailleDp);
            final float nouveauPx = dpToPx(tailleDp);

            ValueAnimator anim = ValueAnimator.ofFloat(ancienPx, nouveauPx);
            anim.setDuration(220);
            anim.setInterpolator(new DecelerateInterpolator());
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int taillePx = (int) (float) animation.getAnimatedValue();
                    for (ImageView iv : vuesIcones) {
                        ViewGroup.LayoutParams p = iv.getLayoutParams();
                        p.width = taillePx;
                        p.height = taillePx;
                        iv.setLayoutParams(p);
                    }
                }
            });
            anim.start();

        } catch (Exception e) {
            NavBarError("Erreur NavBarSetIconSize: " + e.getMessage());
        }
    }

    @SimpleFunction(description = "Sélectionne un onglet de la barre par code, sans clic.")
    public void NavBarSelect(String id) {
        int index = idsFinaux.indexOf(id);
        if (index < 0) {
            NavBarError("Id introuvable pour NavBarSelect: " + id);
            return;
        }
        SelectionnerOnglet(id, vuesCercles.get(index), vuesIcones.get(index));
    }

    private void SelectionnerOnglet(String id, View cercle, ImageView img) {
        try {
            if (id.equals(idSelectionne)) return;

            if (idSelectionne != null) {
                int ancienIndex = idsFinaux.indexOf(idSelectionne);
                if (ancienIndex >= 0) {
                    animerOnglet(vuesCercles.get(ancienIndex), vuesIcones.get(ancienIndex), false);
                }
            }

            animerOnglet(cercle, img, true);
            idSelectionne = id;
            OnSelected(id);

        } catch (Exception e) {
            NavBarError("Erreur de sélection: " + e.getMessage());
        }
    }

    private void animerOnglet(final View cercle, final ImageView img, boolean selectionne) {
        float alphaCible = selectionne ? 1f : 0f;
        ValueAnimator anim = ValueAnimator.ofFloat(cercle.getAlpha(), alphaCible);
        anim.setDuration(220);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                cercle.setAlpha(val);
                int couleur = melangerCouleurs(Color.rgb(150, 150, 150), Color.rgb(20, 20, 20), val);
                img.setColorFilter(new PorterDuffColorFilter(couleur, PorterDuff.Mode.SRC_IN));
            }
        });
        anim.start();
    }

    private int melangerCouleurs(int c1, int c2, float ratio) {
        int r = (int) (Color.red(c1) + ratio * (Color.red(c2) - Color.red(c1)));
        int g = (int) (Color.green(c1) + ratio * (Color.green(c2) - Color.green(c1)));
        int b = (int) (Color.blue(c1) + ratio * (Color.blue(c2) - Color.blue(c1)));
        return Color.rgb(r, g, b);
    }

    private float dpToPx(int dp) {
        float density = activity.getResources().getDisplayMetrics().density;
        return dp * density;
    }

    // =========================================================================
    // 1. MOTEUR DE CHAT & DÉFILEMENT AUTOMATIQUE
    // =========================================================================

    @SimpleFunction(description = "Ajoute une bulle de message dynamique en utilisant un Template Kodular.")
    public void AddChatMessageFromTemplate(
            final AndroidViewComponent chatContainer,
            final AndroidViewComponent templateBubbleCard,
            final String messageText,
            final String timestamp,
            final String senderUid,
            final boolean isMe,
            final int bubbleColor,
            final String avatarUrl) {

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    View containerView = chatContainer.getView();
                    View templateView = templateBubbleCard.getView();

                    if (containerView instanceof ViewGroup) {
                        ViewGroup parentLayout = (ViewGroup) containerView;

                        templateView.setBackgroundColor(bubbleColor);

                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        params.gravity = isMe ? Gravity.END : Gravity.START;
                        params.setMargins(12, 8, 12, 8);
                        templateView.setLayoutParams(params);

                        templateView.setTag(senderUid);
                        templateView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                OnUserAvatarClick(senderUid, isMe ? "me" : "user");
                            }
                        });

                        parentLayout.addView(templateView);
                        ScrollToBottom(chatContainer);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @SimpleFunction(description = "Fait défiler le ScrollArrangement jusqu'au tout dernier message.")
    public void ScrollToBottom(final AndroidViewComponent scrollContainer) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View view = scrollContainer.getView();
                if (view instanceof ScrollView) {
                    final ScrollView scrollView = (ScrollView) view;
                    scrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollView.fullScroll(View.FOCUS_DOWN);
                        }
                    });
                }
            }
        });
    }

    // =========================================================================
    // 2. SAISIE FLOTTANTE & CLAVIER
    // =========================================================================

    @SimpleFunction(description = "Attache la zone de saisie au-dessus du clavier.")
    public void AttachFloatingInputWithDynamicHeight(
            final AndroidViewComponent inputContainer,
            final AndroidViewComponent editTextComponent,
            final int maxHeightPx) {

        final View containerView = inputContainer.getView();
        final View rootView = activity.getWindow().getDecorView().getRootView();

        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                android.graphics.Rect r = new android.graphics.Rect();
                rootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootView.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                if (keypadHeight > screenHeight * 0.15) {
                    containerView.setTranslationY(-keypadHeight);
                } else {
                    containerView.setTranslationY(0);
                }
            }
        });
    }

    // =========================================================================
    // 3. CATALOGUE DE PRODUITS
    // =========================================================================

    @SimpleFunction(description = "Construit la grille de produits depuis un JSON.")
    public void BuildProductGridFromJson(
            final AndroidViewComponent scrollContainer,
            final AndroidViewComponent templateCard,
            final String jsonData) {

        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                try {
                    final JSONArray jsonArray = new JSONArray(jsonData);

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                View containerView = scrollContainer.getView();
                                if (containerView instanceof ViewGroup) {
                                    ViewGroup layout = (ViewGroup) containerView;

                                    for (int i = 0; i < jsonArray.length(); i++) {
                                        JSONObject item = jsonArray.getJSONObject(i);
                                        final String productUid = item.optString("uid", "");

                                        View cardClone = templateCard.getView();
                                        cardClone.setTag(productUid);
                                        cardClone.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                OnProductCardClick(productUid);
                                            }
                                        });

                                        layout.addView(cardClone);
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // =========================================================================
    // 3B. LISTE DYNAMIQUE DE CATÉGORIES (construite nativement, sans clonage)
    // =========================================================================

    @SimpleFunction(description = "Génère la liste des catégories/sous-catégories depuis un JSON. sectionColor/itemColor: couleurs du texte.")
    public void BuildCategoryListFromJson(
            final AndroidViewComponent listContainer,
            final int sectionColor,
            final int itemColor,
            final String categoriesJson) {

        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                try {
                    final JSONArray mainArray = new JSONArray(categoriesJson);

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                View containerView = listContainer.getView();
                                if (!(containerView instanceof ViewGroup)) return;

                                ViewGroup layout = (ViewGroup) containerView;
                                layout.removeAllViews();

                                RadioGroup groupeUnique = new RadioGroup(activity);
                                groupeUnique.setOrientation(LinearLayout.VERTICAL);

                                for (int i = 0; i < mainArray.length(); i++) {
                                    JSONObject categoryObj = mainArray.getJSONObject(i);
                                    String categoryName = categoryObj.optString("title", "");
                                    JSONArray subCategories = categoryObj.optJSONArray("subcategories");

                                    TextView header = new TextView(activity);
                                    header.setText("›  " + categoryName);
                                    header.setTextColor(sectionColor);
                                    header.setTextSize(18);
                                    header.setTypeface(header.getTypeface(), Typeface.BOLD);
                                    int padPx = (int) dpToPx(12);
                                    header.setPadding(padPx, padPx, padPx, (int) dpToPx(6));
                                    layout.addView(header);

                                    if (subCategories != null) {
                                        for (int j = 0; j < subCategories.length(); j++) {
                                            JSONObject subObj = subCategories.getJSONObject(j);
                                            final String subId = subObj.optString("id", "");
                                            final String subTitle = subObj.optString("title", "");

                                            RadioButton bouton = new RadioButton(activity);
                                            bouton.setText(subTitle);
                                            bouton.setTextColor(itemColor);
                                            bouton.setTextSize(16);
                                            bouton.setPadding((int) dpToPx(16), (int) dpToPx(10), (int) dpToPx(16), (int) dpToPx(10));
                                            bouton.setTag(subId);

                                            bouton.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    OnCategorySelected(subId, subTitle);
                                                }
                                            });

                                            groupeUnique.addView(bouton);
                                        }
                                    }
                                }

                                layout.addView(groupeUnique);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // =========================================================================
    // 4. EFFETS VISUELS (DÉGRADÉ & FLOU GLASSMORPHISM)
    // =========================================================================

    @SimpleFunction(description = "Applique un dégradé de couleur sur un composant.")
    public void SetGradientBackground(
            final AndroidViewComponent component,
            final int startColor,
            final int endColor,
            final String orientation) {

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    GradientDrawable.Orientation gradOrientation = GradientDrawable.Orientation.TOP_BOTTOM;
                    if ("LEFT_RIGHT".equalsIgnoreCase(orientation)) {
                        gradOrientation = GradientDrawable.Orientation.LEFT_RIGHT;
                    }

                    GradientDrawable gd = new GradientDrawable(
                            gradOrientation,
                            new int[]{startColor, endColor}
                    );
                    gd.setCornerRadius(0f);
                    component.getView().setBackground(gd);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @SimpleFunction(description = "Applique un effet de flou (Glassmorphism) sur un composant.")
    public void SetBlurEffect(final AndroidViewComponent component, final float radius) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    View view = component.getView();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        float blurRadius = Math.max(1f, Math.min(radius, 25f));
                        view.setRenderEffect(RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP));
                    } else {
                        view.setBackgroundColor(Color.argb(150, 255, 255, 255));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // =========================================================================
    // 5. DIALOGUE TRANSPARENT, NOTIFICATEUR & GESTION SONORE
    // =========================================================================

    @SimpleFunction(description = "Affiche un composant sous forme de dialogue transparent.")
    public void ShowAlphaDialog(final AndroidViewComponent dialogContentLayout, final boolean cancelable) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (activeAlphaDialog != null && activeAlphaDialog.isShowing()) {
                        activeAlphaDialog.dismiss();
                    }

                    activeAlphaDialog = new Dialog(activity);
                    activeAlphaDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

                    View contentView = dialogContentLayout.getView();
                    if (contentView.getParent() != null) {
                        ((ViewGroup) contentView.getParent()).removeView(contentView);
                    }

                    activeAlphaDialog.setContentView(contentView);

                    if (activeAlphaDialog.getWindow() != null) {
                        activeAlphaDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                        activeAlphaDialog.getWindow().setLayout(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        );
                    }

                    activeAlphaDialog.setCancelable(cancelable);
                    activeAlphaDialog.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @SimpleFunction(description = "Ferme le dialogue Alpha.")
    public void DismissAlphaDialog() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (activeAlphaDialog != null && activeAlphaDialog.isShowing()) {
                    activeAlphaDialog.dismiss();
                    activeAlphaDialog = null;
                }
            }
        });
    }

    @SimpleFunction(description = "Notification personnalisée temporaire.")
    public void CustomNotifier(final AndroidViewComponent customLayout, final int durationMs) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ShowAlphaDialog(customLayout, true);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        DismissAlphaDialog();
                    }
                }, durationMs);
            }
        });
    }

    @SimpleFunction(description = "Joue un son personnalisé (ex: envoi de message, notification).")
    public void PlayCustomSound(final String fileNameOrPath) {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                MediaPlayer mediaPlayer = null;
                try {
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setAudioAttributes(
                            new AudioAttributes.Builder()
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                                    .build()
                    );

                    if (fileNameOrPath.startsWith("/")) {
                        mediaPlayer.setDataSource(fileNameOrPath);
                    } else {
                        android.content.res.AssetFileDescriptor afd = context.getAssets().openFd(fileNameOrPath);
                        mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        afd.close();
                    }

                    mediaPlayer.prepare();
                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            mp.release();
                        }
                    });
                    mediaPlayer.start();
                } catch (Exception e) {
                    e.printStackTrace();
                    if (mediaPlayer != null) {
                        mediaPlayer.release();
                    }
                }
            }
        });
    }

    // =========================================================================
    // 6. GALERIE D'IMAGES & COMPRESSION
    // =========================================================================

    @SimpleFunction(description = "Ouvre la galerie d'images native.")
    public void OpenPhotoPicker() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.setType("image/*");
                    activity.startActivityForResult(intent, PICK_IMAGE_REQUEST);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void resultReturned(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                OnPhotoPicked(selectedImageUri.toString());
            }
        }
    }

    @SimpleFunction(description = "Compresse une image sans surcharger la mémoire.")
    public String CompressImage(String imagePath, int quality, int maxWidth) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imagePath, options);

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return imagePath;
            }

            int srcWidth = options.outWidth;
            int inSampleSize = 1;

            if (srcWidth > maxWidth) {
                inSampleSize = Math.round((float) srcWidth / (float) maxWidth);
            }

            options.inJustDecodeBounds = false;
            options.inSampleSize = inSampleSize;

            Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);
            if (bitmap == null) {
                return imagePath;
            }

            File outputFile = new File(context.getCacheDir(), "comp_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(outputFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
                out.flush();
            } finally {
                if (out != null) {
                    out.close();
                }
                bitmap.recycle();
            }

            return outputFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return imagePath;
        }
    }

    // =========================================================================
    // 7. REQUÊTES SERVEUR (ASYNCHRONE, sans dépendance externe)
    // =========================================================================

    @SimpleFunction(description = "Envoie une requête HTTPS au serveur.")
    public void CallServerRequest(final String endpointUrl, final String method, final String headersJson, final String bodyJson) {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(endpointUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);
                    conn.setRequestMethod("POST".equalsIgnoreCase(method) ? "POST" : "GET");

                    if (headersJson != null && !headersJson.isEmpty()) {
                        JSONObject headers = new JSONObject(headersJson);
                        Iterator<String> keys = headers.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            conn.setRequestProperty(key, headers.getString(key));
                        }
                    }

                    if ("POST".equalsIgnoreCase(method)) {
                        conn.setDoOutput(true);
                        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                        if (bodyJson != null) {
                            OutputStream os = conn.getOutputStream();
                            os.write(bodyJson.getBytes("UTF-8"));
                            os.flush();
                            os.close();
                        }
                    }

                    final int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 400) ? conn.getInputStream() : conn.getErrorStream();
                    final String responseContent = lireFlux(is);

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            OnServerResponse(responseCode, responseContent);
                        }
                    });

                } catch (final Exception e) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            OnServerResponse(500, e.getMessage());
                        }
                    });
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
        });
    }

    private String lireFlux(InputStream is) throws IOException {
        if (is == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String ligne;
        while ((ligne = reader.readLine()) != null) {
            sb.append(ligne);
        }
        reader.close();
        return sb.toString();
    }

    // =========================================================================
    // 8. ÉVÉNEMENTS (EVENTS) KODULAR
    // =========================================================================

    @SimpleEvent(description = "Déclenché quand l'utilisateur touche une icône de la barre de navigation.")
    public void OnSelected(String id) {
        EventDispatcher.dispatchEvent(this, "OnSelected", id);
    }

    @SimpleEvent(description = "Déclenché en cas de problème avec la barre de navigation.")
    public void NavBarError(String message) {
        EventDispatcher.dispatchEvent(this, "NavBarError", message);
    }

    @SimpleEvent(description = "Déclenché lors du clic sur un produit.")
    public void OnProductCardClick(String productUid) {
        EventDispatcher.dispatchEvent(this, "OnProductCardClick", productUid);
    }

    @SimpleEvent(description = "Déclenché lors du choix d'une catégorie. Renvoie l'ID et le Nom.")
    public void OnCategorySelected(String categoryId, String categoryTitle) {
        EventDispatcher.dispatchEvent(this, "OnCategorySelected", categoryId, categoryTitle);
    }

    @SimpleEvent(description = "Déclenché lors du clic sur l'avatar.")
    public void OnUserAvatarClick(String userUid, String userRole) {
        EventDispatcher.dispatchEvent(this, "OnUserAvatarClick", userUid, userRole);
    }

    @SimpleEvent(description = "Déclenché après sélection d'une image.")
    public void OnPhotoPicked(String imageUri) {
        EventDispatcher.dispatchEvent(this, "OnPhotoPicked", imageUri);
    }

    @SimpleEvent(description = "Déclenché après réponse du serveur.")
    public void OnServerResponse(int responseCode, String responseContent) {
        EventDispatcher.dispatchEvent(this, "OnServerResponse", responseCode, responseContent);
    }
}
