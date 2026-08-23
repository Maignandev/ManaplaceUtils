package com.devkit.utils;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.MediaUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
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
        version = 12,
        description = "Extension ManaplaceUtils - corrections: clavier flottant, auto-grow, galerie/permissions, barre de navigation.",
        category = ComponentCategory.EXTENSION,
        nonVisible = true
)
@SimpleObject(external = true)
@UsesPermissions(
        permissionNames =
                "android.permission.READ_EXTERNAL_STORAGE," +
                "android.permission.READ_MEDIA_IMAGES," +
                "android.permission.INTERNET"
)
public class ManaplaceUtils extends AndroidNonvisibleComponent implements ActivityResultListener {

    private final Context context;
    private final Activity activity;
    private final Form monForm;
    private final int requestCode;
    private Dialog activeAlphaDialog;

    private Typeface customTypeface = Typeface.DEFAULT;
    private int radioButtonColor = Color.parseColor("#C01A1A1B");

    // =========================================================================
    // 0. BARRE DE NAVIGATION FLOTTANTE
    // =========================================================================

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
        this.requestCode = this.form.registerForActivityResult(this);
    }

    private float dpToPx(int dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }

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
    public void NavBarInitialize(final int margeBas, final double largeurPourcent, final double hauteurPourcent) {
        if (dejaInitialise) return;

        if (idsEnAttente.isEmpty()) {
            NavBarError("Aucune icône ajoutée — appelle NavBarAdd avant NavBarInitialize");
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    FrameLayout root = (FrameLayout) activity.findViewById(android.R.id.content);
                    if (root == null) {
                        NavBarError("Écran racine introuvable");
                        return;
                    }

                    DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
                    int largeurFinale = (largeurPourcent > 0)
                            ? (int) (metrics.widthPixels * (largeurPourcent / 100.0))
                            : ViewGroup.LayoutParams.WRAP_CONTENT;
                    int hauteurFinale = (hauteurPourcent > 0)
                            ? (int) (metrics.heightPixels * (hauteurPourcent / 100.0))
                            : (int) dpToPx(64);

                    LinearLayout bar = new LinearLayout(activity);
                    bar.setOrientation(LinearLayout.HORIZONTAL);
                    bar.setGravity(Gravity.CENTER);
                    bar.setWeightSum(idsEnAttente.size());
                    bar.setElevation(dpToPx(12));

                    GradientDrawable fond = new GradientDrawable();
                    fond.setColor(Color.WHITE);
                    fond.setCornerRadius(dpToPx(30));
                    bar.setBackground(fond);

                    for (int i = 0; i < idsEnAttente.size(); i++) {
                        final String tabId = idsEnAttente.get(i);
                        String iconFile = iconesEnAttente.get(i);

                        FrameLayout conteneur = new FrameLayout(activity);
                        conteneur.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

                        View cercle = new View(activity);
                        GradientDrawable fondCercle = new GradientDrawable();
                        fondCercle.setShape(GradientDrawable.OVAL);
                        fondCercle.setColor(Color.argb(30, 0, 0, 0));
                        cercle.setBackground(fondCercle);
                        cercle.setAlpha(0f);
                        conteneur.addView(cercle, new FrameLayout.LayoutParams((int) dpToPx(46), (int) dpToPx(46), Gravity.CENTER));

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
                        conteneur.addView(img, new FrameLayout.LayoutParams(taillePx, taillePx, Gravity.CENTER));

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
                    NavBarError("NavBarInitialize: " + e.getMessage());
                }
            }
        });
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

                int couleur = melangerCouleurs(
                        Color.rgb(150, 150, 150),
                        Color.rgb(20, 20, 20),
                        val
                );

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

    // =========================================================================
    // POLICE PERSONNALISÉE
    // =========================================================================

    @SimpleFunction(description = "Charge une police personnalisée .ttf ou .otf.")
    public void LoadCustomFont(String fontPath) {
        try {
            if (fontPath == null || fontPath.trim().isEmpty()) {
                customTypeface = Typeface.DEFAULT;
                return;
            }

            if (fontPath.startsWith("/")) {
                customTypeface = Typeface.createFromFile(new File(fontPath));
            } else {
                customTypeface = Typeface.createFromAsset(context.getAssets(), fontPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
            customTypeface = Typeface.DEFAULT;
        }
    }

    @SimpleFunction(description = "Définit la couleur des boutons radio.")
    public void SetRadioButtonColor(int color) {
        radioButtonColor = color;
    }

    // =========================================================================
    // UTILITAIRES IMAGES
    // =========================================================================

    private ViewGroup getRealLayout(AndroidViewComponent component) {
        if (component == null) return null;
        View view = component.getView();

        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            if (vg.getChildCount() > 0 && vg.getChildAt(0) instanceof ViewGroup) {
                return (ViewGroup) vg.getChildAt(0);
            }
            return vg;
        }
        return null;
    }

    private void runOnUi(Runnable runnable) {
        activity.runOnUiThread(runnable);
    }

    private void loadImageAsync(final ImageView imageView, final String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) return;

        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                Bitmap bmp = null;
                InputStream input = null;
                HttpURLConnection conn = null;

                try {
                    if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                        URL url = new URL(imagePath);
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setConnectTimeout(15000);
                        conn.setReadTimeout(15000);
                        conn.setDoInput(true);
                        conn.connect();
                        input = conn.getInputStream();
                        bmp = BitmapFactory.decodeStream(input);

                    } else if (imagePath.startsWith("content://")) {
                        input = context.getContentResolver().openInputStream(Uri.parse(imagePath));
                        if (input != null) {
                            bmp = BitmapFactory.decodeStream(input);
                        }

                    } else {
                        try {
                            input = context.getAssets().open(imagePath);
                            bmp = BitmapFactory.decodeStream(input);
                        } catch (Exception assetError) {
                            try {
                                bmp = MediaUtil.getBitmapDrawable(monForm, imagePath).getBitmap();
                            } catch (Exception mediaError) {
                                File file = new File(imagePath);
                                if (file.exists()) {
                                    input = new FileInputStream(file);
                                    bmp = BitmapFactory.decodeStream(input);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (input != null) {
                        try { input.close(); } catch (Exception ignored) {}
                    }
                    if (conn != null) {
                        conn.disconnect();
                    }
                }

                final Bitmap finalBmp = bmp;
                if (finalBmp != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (imageView.getWindowToken() != null || imageView.isAttachedToWindow()) {
                                imageView.setImageBitmap(finalBmp);
                            }
                        }
                    });
                }
            }
        });
    }

    // =========================================================================
    // 1. MOTEUR DE CHAT DYNAMIQUE NATIF
    // =========================================================================

    @SimpleFunction(description = "Ajoute une bulle de chat avec un petit avatar rond.")
    public void AddChatBubble(
            final AndroidViewComponent chatContainer,
            final String messageText,
            final String timeText,
            final String avatarUrl,
            final boolean isMe,
            final int bubbleColor,
            final int textColor) {

        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    ViewGroup targetLayout = getRealLayout(chatContainer);
                    if (targetLayout == null) return;

                    int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;

                    LinearLayout row = new LinearLayout(context);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(isMe ? Gravity.END : Gravity.START);

                    LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    rowParams.setMargins((int) dpToPx(8), (int) dpToPx(4), (int) dpToPx(8), (int) dpToPx(4));
                    row.setLayoutParams(rowParams);

                    int avatarSizePx = (int) dpToPx(32);
                    CardView avatarCard = new CardView(context);
                    LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(avatarSizePx, avatarSizePx);
                    avatarParams.gravity = Gravity.CENTER_VERTICAL;
                    avatarParams.setMargins((int) dpToPx(6), 0, (int) dpToPx(6), 0);
                    avatarCard.setLayoutParams(avatarParams);
                    avatarCard.setRadius(avatarSizePx / 2f);
                    avatarCard.setCardElevation(0f);
                    avatarCard.setMaxCardElevation(0f);
                    avatarCard.setCardBackgroundColor(Color.parseColor("#E0E0E0"));

                    ImageView avatarImg = new ImageView(context);
                    avatarImg.setLayoutParams(new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    ));
                    avatarImg.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    avatarImg.setPadding((int) dpToPx(4), (int) dpToPx(4), (int) dpToPx(4), (int) dpToPx(4));

                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        loadImageAsync(avatarImg, avatarUrl);
                    }

                    avatarCard.addView(avatarImg);
                    avatarCard.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            OnAvatarClick(isMe);
                        }
                    });

                    LinearLayout bubble = new LinearLayout(context);
                    bubble.setOrientation(LinearLayout.VERTICAL);
                    bubble.setPadding((int) dpToPx(16), (int) dpToPx(10), (int) dpToPx(16), (int) dpToPx(10));

                    GradientDrawable bg = new GradientDrawable();
                    bg.setShape(GradientDrawable.RECTANGLE);
                    bg.setColor(bubbleColor);
                    bg.setCornerRadius(dpToPx(22));
                    bubble.setBackground(bg);

                    int maxBubbleWidth = (int) (screenWidth * 0.72);
                    LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    bubble.setLayoutParams(bubbleParams);

                    TextView msgTv = new TextView(context);
                    msgTv.setText(messageText);
                    msgTv.setTextColor(textColor);
                    msgTv.setTextSize(15);
                    msgTv.setMaxWidth(maxBubbleWidth);
                    msgTv.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    ));

                    if (customTypeface != null) {
                        msgTv.setTypeface(customTypeface);
                    }

                    bubble.addView(msgTv);

                    if (timeText != null && !timeText.isEmpty()) {
                        TextView timeTv = new TextView(context);
                        timeTv.setText(timeText);
                        timeTv.setTextColor(Color.argb(
                                180,
                                Color.red(textColor),
                                Color.green(textColor),
                                Color.blue(textColor)
                        ));
                        timeTv.setTextSize(10);

                        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        timeParams.gravity = Gravity.END;
                        timeParams.setMargins(0, (int) dpToPx(2), 0, 0);
                        timeTv.setLayoutParams(timeParams);

                        if (customTypeface != null) {
                            timeTv.setTypeface(customTypeface);
                        }

                        bubble.addView(timeTv);
                    }

                    if (isMe) {
                        row.addView(bubble);
                        row.addView(avatarCard);
                    } else {
                        row.addView(avatarCard);
                        row.addView(bubble);
                    }

                    targetLayout.addView(row);
                    ScrollToBottom(chatContainer);

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
    // 2. SAISIE FLOTTANTE & CLAVIER (REMPLACÉ)
    // =========================================================================

    @SimpleFunction(
            description = "Attache la zone de saisie au-dessus du clavier de manière flottante."
    )
    public void AttachFloatingInputWithDynamicHeight(
            final Object inputContainer,
            final Object editTextComponent,
            final int maxHeightPx) {

        if (!(inputContainer instanceof AndroidViewComponent)) {
            return;
        }

        final View containerView =
                ((AndroidViewComponent) inputContainer).getView();

        if (containerView == null) {
            return;
        }

        final View rootView =
                activity.getWindow()
                        .getDecorView()
                        .getRootView();

        rootView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {

                    @Override
                    public void onGlobalLayout() {

                        Rect r = new Rect();

                        rootView.getWindowVisibleDisplayFrame(r);

                        int screenHeight =
                                rootView.getRootView().getHeight();

                        int keypadHeight =
                                screenHeight - r.bottom;

                        if (keypadHeight > screenHeight * 0.15) {

                            containerView.setTranslationY(
                                    -keypadHeight
                            );

                        } else {

                            containerView.setTranslationY(0);
                        }
                    }
                }
        );
    }

    @SimpleFunction(
            description = "Force le conteneur à s'agrandir dynamiquement quand le TextBox multiligne à l'intérieur grandit."
    )
    public void EnableAutoGrowWithText(
            final AndroidViewComponent cardContainer,
            final AndroidViewComponent editTextComponent) {

        if (cardContainer == null ||
                editTextComponent == null) {
            return;
        }

        final View containerView =
                cardContainer.getView();

        final View editView =
                editTextComponent.getView();

        if (containerView == null ||
                !(editView instanceof EditText)) {
            return;
        }

        ((EditText) editView).addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s) {

                        containerView.requestLayout();

                        View parent =
                                (View) containerView.getParent();

                        if (parent != null) {
                            parent.requestLayout();
                        }
                    }
                }
        );
    }

    // =========================================================================
    // 3. CATALOGUE DE PRODUITS 2x2 NATIF
    // =========================================================================

    @SimpleFunction(description = "Construit la grille de produits depuis un JSON sans élévation.")
    public void BuildProductGridFromJson(
            final AndroidViewComponent scrollContainer,
            final String jsonData) {

        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                try {
                    final JSONArray array = new JSONArray(jsonData);
                    final int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
                    final int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;

                    runOnUi(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                ViewGroup targetLayout = getRealLayout(scrollContainer);
                                if (targetLayout == null) return;

                                targetLayout.removeAllViews();

                                int cardWidth = (int) (screenWidth * 0.44);
                                int cardHeight = (int) (screenHeight * 0.28);

                                LinearLayout currentRow = null;

                                for (int i = 0; i < array.length(); i++) {
                                    JSONObject item = array.getJSONObject(i);

                                    final String uid = item.optString("uid", String.valueOf(i));
                                    String imageStr = item.optString("image", "");
                                    String titleStr = item.optString("title", "");
                                    String priceStr = item.optString("price", "");

                                    if (i % 2 == 0) {
                                        currentRow = new LinearLayout(context);
                                        currentRow.setOrientation(LinearLayout.HORIZONTAL);
                                        currentRow.setGravity(Gravity.CENTER_HORIZONTAL);

                                        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                                                LinearLayout.LayoutParams.MATCH_PARENT,
                                                LinearLayout.LayoutParams.WRAP_CONTENT
                                        );
                                        rowParams.setMargins(0, 8, 0, 8);
                                        currentRow.setLayoutParams(rowParams);

                                        targetLayout.addView(currentRow);
                                    }

                                    CardView card = new CardView(context);
                                    LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                                            cardWidth,
                                            cardHeight
                                    );
                                    cardParams.setMargins(10, 8, 10, 8);
                                    card.setLayoutParams(cardParams);
                                    card.setRadius(20f);
                                    card.setCardBackgroundColor(Color.WHITE);

                                    LinearLayout inner = new LinearLayout(context);
                                    inner.setOrientation(LinearLayout.VERTICAL);
                                    inner.setBackgroundColor(Color.WHITE);
                                    inner.setLayoutParams(new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.MATCH_PARENT
                                    ));

                                    ImageView img = new ImageView(context);
                                    LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            0,
                                            1.0f
                                    );
                                    img.setLayoutParams(imgParams);
                                    img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                    img.setBackgroundColor(Color.parseColor("#F5F5F5"));

                                    loadImageAsync(img, imageStr);
                                    inner.addView(img);

                                    TextView titleTv = new TextView(context);
                                    titleTv.setText(titleStr);
                                    titleTv.setTextColor(Color.BLACK);
                                    titleTv.setTextSize(13);
                                    titleTv.setMaxLines(2);
                                    titleTv.setPadding(14, 8, 14, 0);

                                    if (customTypeface != null) {
                                        titleTv.setTypeface(customTypeface);
                                    }
                                    inner.addView(titleTv);

                                    TextView priceTv = new TextView(context);
                                    priceTv.setText(priceStr);
                                    priceTv.setTextColor(Color.BLACK);
                                    priceTv.setTextSize(14);
                                    priceTv.setTypeface(null, Typeface.BOLD);
                                    priceTv.setPadding(14, 2, 14, 12);

                                    if (customTypeface != null) {
                                        priceTv.setTypeface(customTypeface, Typeface.BOLD);
                                    }
                                    inner.addView(priceTv);

                                    card.addView(inner);

                                    card.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            OnProductCardClick(uid);
                                        }
                                    });

                                    if (currentRow != null) {
                                        currentRow.addView(card);
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
    // 3B. LISTE DYNAMIQUE DE CATÉGORIES
    // =========================================================================

    @SimpleFunction(description = "Génère la liste des catégories/sous-catégories depuis un JSON.")
    public void BuildCategoryListFromJson(
            final AndroidViewComponent listContainer,
            final String categoriesJson) {

        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                try {
                    final JSONArray mainArray = new JSONArray(categoriesJson);

                    runOnUi(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                ViewGroup target = getRealLayout(listContainer);
                                if (target == null) return;

                                target.removeAllViews();

                                RadioGroup group = new RadioGroup(activity);
                                group.setOrientation(LinearLayout.VERTICAL);

                                ColorStateList radioColors = ColorStateList.valueOf(radioButtonColor);

                                for (int i = 0; i < mainArray.length(); i++) {
                                    JSONObject category = mainArray.getJSONObject(i);
                                    String categoryName = category.optString("title", "");
                                    JSONArray subCategories = category.optJSONArray("subcategories");

                                    TextView header = new TextView(activity);
                                    header.setText(">  " + categoryName);
                                    header.setTextColor(Color.parseColor("#E91A1A1B"));
                                    header.setTextSize(18);
                                    header.setTypeface(customTypeface, Typeface.BOLD);
                                    header.setPadding(0, (int) dpToPx(16), 0, (int) dpToPx(8));

                                    group.addView(header);

                                    if (subCategories != null) {
                                        for (int j = 0; j < subCategories.length(); j++) {
                                            JSONObject sub = subCategories.getJSONObject(j);
                                            final String id = sub.optString("id", "");
                                            final String title = sub.optString("title", "");

                                            RadioButton button = new RadioButton(activity);
                                            button.setId(View.generateViewId());
                                            button.setText(title);
                                            button.setTextColor(Color.parseColor("#C01A1A1B"));
                                            button.setTextSize(13);

                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                                button.setButtonTintList(radioColors);
                                            }

                                            if (customTypeface != null) {
                                                button.setTypeface(customTypeface);
                                            }

                                            button.setPadding(
                                                    (int) dpToPx(8),
                                                    (int) dpToPx(12),
                                                    (int) dpToPx(8),
                                                    (int) dpToPx(12)
                                            );

                                            button.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    OnCategorySelected(id, title);
                                                }
                                            });

                                            group.addView(button);

                                            View divider = new View(activity);
                                            divider.setLayoutParams(new LinearLayout.LayoutParams(
                                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                                    (int) dpToPx(1)
                                            ));
                                            divider.setBackgroundColor(Color.parseColor("#F0F0F0"));

                                            group.addView(divider);
                                        }
                                    }
                                }

                                target.addView(group);

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
    // 4. EFFETS VISUELS
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
                        view.setRenderEffect(
                                RenderEffect.createBlurEffect(
                                        blurRadius,
                                        blurRadius,
                                        Shader.TileMode.CLAMP
                                )
                        );
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
    // 5. DIALOGUE TRANSPARENT, NOTIFICATION & GESTION SONORE
    // =========================================================================

    @SimpleFunction(description = "Affiche un composant sous forme de dialogue transparent.")
    public void ShowAlphaDialog(
            final AndroidViewComponent dialogContentLayout,
            final boolean cancelable) {

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
                        mediaPlayer.setDataSource(
                                afd.getFileDescriptor(),
                                afd.getStartOffset(),
                                afd.getLength()
                        );
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

    @SimpleFunction(description = "Ouvre la galerie d'images native, avec demande de permission.")
    public void OpenPhotoPicker() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = "android.permission.READ_MEDIA_IMAGES";
        } else {
            permission = "android.permission.READ_EXTERNAL_STORAGE";
        }

        form.askPermission(permission, new PermissionResultHandler() {
            @Override
            public void HandlePermissionResponse(String permissionName, boolean granted) {
                if (!granted) {
                    OnError("Permission refusée.");
                    return;
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Intent intent = new Intent(Intent.ACTION_PICK);
                            intent.setType("image/*");
                            form.startActivityForResult(intent, requestCode);
                        } catch (Exception e) {
                            OnError("OpenPhotoPicker: " + e.getMessage());
                        }
                    }
                });
            }
        });
    }

    @Override
    public void resultReturned(int receivedRequestCode, int resultCode, Intent data) {
        if (receivedRequestCode != requestCode) return;

        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                OnPhotoPicked(selectedImageUri.toString());
            } else {
                OnError("URI nulle.");
            }
        } else {
            OnError("Sélection annulée.");
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
            if (bitmap == null) return imagePath;

            File outputFile = new File(
                    context.getCacheDir(),
                    "comp_" + System.currentTimeMillis() + ".jpg"
            );

            FileOutputStream out = null;
            try {
                out = new FileOutputStream(outputFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
                out.flush();
            } finally {
                if (out != null) out.close();
                bitmap.recycle();
            }

            return outputFile.getAbsolutePath();

        } catch (Exception e) {
            e.printStackTrace();
            return imagePath;
        }
    }

    // =========================================================================
    // 7. REQUÊTES SERVEUR
    // =========================================================================

    @SimpleFunction(description = "Envoie une requête HTTPS au serveur.")
    public void CallServerRequest(
            final String endpointUrl,
            final String method,
            final String headersJson,
            final String bodyJson) {

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
                    InputStream is = (responseCode >= 200 && responseCode < 400)
                            ? conn.getInputStream()
                            : conn.getErrorStream();

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
                    if (conn != null) conn.disconnect();
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
    // 8. ÉVÉNEMENTS KODULAR
    // =========================================================================

    @SimpleEvent(description = "Déclenché quand l'utilisateur touche une icône de la barre de navigation.")
    public void OnSelected(String id) {
        EventDispatcher.dispatchEvent(this, "OnSelected", id);
    }

    @SimpleEvent(description = "Déclenché en cas de problème avec la barre de navigation.")
    public void NavBarError(String message) {
        EventDispatcher.dispatchEvent(this, "NavBarError", message);
    }

    @SimpleEvent(description = "Déclenché lors du clic sur une carte produit.")
    public void OnProductCardClick(String productUid) {
        EventDispatcher.dispatchEvent(this, "OnProductCardClick", productUid);
    }

    @SimpleEvent(description = "Déclenché lors du choix d'une catégorie. Renvoie l'ID et le Nom.")
    public void OnCategorySelected(String categoryId, String categoryTitle) {
        EventDispatcher.dispatchEvent(this, "OnCategorySelected", categoryId, categoryTitle);
    }

    @SimpleEvent(description = "Déclenché lors du clic sur l'avatar du message.")
    public void OnAvatarClick(boolean isMe) {
        EventDispatcher.dispatchEvent(this, "OnAvatarClick", isMe);
    }

    @SimpleEvent(description = "Déclenché après sélection d'une image.")
    public void OnPhotoPicked(String imageUri) {
        EventDispatcher.dispatchEvent(this, "OnPhotoPicked", imageUri);
    }

    @SimpleEvent(description = "Déclenché après réponse du serveur.")
    public void OnServerResponse(int responseCode, String responseContent) {
        EventDispatcher.dispatchEvent(this, "OnServerResponse", responseCode, responseContent);
    }

    @SimpleEvent(description = "Déclenché en cas de problème (permission, sélection...).")
    public void OnError(String message) {
        EventDispatcher.dispatchEvent(this, "OnError", message);
    }
}
