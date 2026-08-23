package com.devkit.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@DesignerComponent(
        version = 8,
        description = "Extension ManaplaceUtils mise à jour pour Kodular.",
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
public class ManaplaceUtils extends AndroidNonvisibleComponent
        implements ActivityResultListener {

    private final Context context;
    private final Activity activity;
    private final Form form;
    private final int requestCode;

    private Typeface customTypeface = Typeface.DEFAULT;
    private int radioButtonColor = Color.parseColor("#C01A1A1B");
    private AlertDialog currentAlphaDialog = null;

    // =========================================================
    // NAVIGATION
    // =========================================================

    private boolean navBarInitialized = false;

    private int navIconSizeDp = 26;

    private final List<String> navIds = new ArrayList<String>();
    private final List<String> navIcons = new ArrayList<String>();

    private final List<ImageView> navImages =
            new ArrayList<ImageView>();

    private final List<View> navCircles =
            new ArrayList<View>();

    private String selectedNavId = null;

    private FrameLayout navBarRoot = null;
    private LinearLayout navBarView = null;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public ManaplaceUtils(ComponentContainer container) {
        super(container.$form());

        this.context = container.$context();
        this.activity = (Activity) container.$context();
        this.form = container.$form();

        this.requestCode =
                form.registerForActivityResult(this);
    }

    // =========================================================
    // UTILITAIRES
    // =========================================================

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }

    private ViewGroup getRealLayout(
            AndroidViewComponent component) {

        if (component == null) {
            return null;
        }

        View view = component.getView();

        if (!(view instanceof ViewGroup)) {
            return null;
        }

        return (ViewGroup) view;
    }

    private void runOnUi(Runnable runnable) {
        activity.runOnUiThread(runnable);
    }

    // =========================================================
    // CHARGEMENT IMAGE ASYNCHRONE
    // =========================================================

    private void loadImageAsync(
            final ImageView imageView,
            final String imagePath) {

        if (imagePath == null ||
                imagePath.trim().isEmpty()) {
            return;
        }

        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {

                Bitmap bmp = null;
                InputStream input = null;
                HttpURLConnection conn = null;

                try {

                    if (imagePath.startsWith("http://") ||
                            imagePath.startsWith("https://")) {

                        URL url = new URL(imagePath);

                        conn = (HttpURLConnection)
                                url.openConnection();

                        conn.setConnectTimeout(15000);
                        conn.setReadTimeout(15000);
                        conn.setDoInput(true);
                        conn.connect();

                        input = conn.getInputStream();

                        bmp = BitmapFactory.decodeStream(input);

                    } else if (imagePath.startsWith("content://")) {

                        input = context
                                .getContentResolver()
                                .openInputStream(
                                        Uri.parse(imagePath)
                                );

                        if (input != null) {
                            bmp = BitmapFactory.decodeStream(input);
                        }

                    } else {

                        try {

                            input = context
                                    .getAssets()
                                    .open(imagePath);

                            bmp = BitmapFactory.decodeStream(input);

                        } catch (Exception assetError) {

                            try {

                                bmp = MediaUtil
                                        .getBitmapDrawable(
                                                form,
                                                imagePath
                                        )
                                        .getBitmap();

                            } catch (Exception mediaError) {

                                File file =
                                        new File(imagePath);

                                if (file.exists()) {

                                    input =
                                            new FileInputStream(file);

                                    bmp =
                                            BitmapFactory
                                                    .decodeStream(input);
                                }
                            }
                        }
                    }

                } catch (Exception e) {

                    e.printStackTrace();

                } finally {

                    if (input != null) {

                        try {
                            input.close();
                        } catch (Exception ignored) {
                        }
                    }

                    if (conn != null) {
                        conn.disconnect();
                    }
                }

                final Bitmap finalBmp = bmp;

                if (finalBmp != null) {

                    activity.runOnUiThread(
                            new Runnable() {
                                @Override
                                public void run() {

                                    if (imageView != null &&
                                            imageView.getWindowToken()
                                                    != null) {

                                        imageView
                                                .setImageBitmap(
                                                        finalBmp
                                                );
                                    }
                                }
                            }
                    );
                }
            }
        });
    }

    // =========================================================
    // CUSTOM FONT
    // =========================================================

    @SimpleFunction(
            description =
                    "Charge une police personnalisée .ttf ou .otf."
    )
    public void LoadCustomFont(String fontPath) {

        try {

            if (fontPath == null ||
                    fontPath.trim().isEmpty()) {

                customTypeface = Typeface.DEFAULT;
                return;
            }

            if (fontPath.startsWith("/")) {

                customTypeface =
                        Typeface.createFromFile(
                                new File(fontPath)
                        );

            } else {

                customTypeface =
                        Typeface.createFromAsset(
                                context.getAssets(),
                                fontPath
                        );
            }

        } catch (Exception e) {

            e.printStackTrace();

            customTypeface = Typeface.DEFAULT;
        }
    }

    // =========================================================
    // RADIO BUTTON
    // =========================================================

    @SimpleFunction(
            description =
                    "Définit la couleur des boutons radio."
    )
    public void SetRadioButtonColor(int color) {

        radioButtonColor = color;
    }

    // =========================================================
    // NAVBAR ADD
    // =========================================================

    @SimpleFunction(
            description =
                    "Ajoute une icône à la barre de navigation."
    )
    public void NavBarAdd(
            AndroidViewComponent container,
            String title,
            String iconName) {

        if (title == null ||
                title.trim().isEmpty()) {

            OnError("NavBarAdd: ID vide.");
            return;
        }

        if (navIds.contains(title)) {

            OnError(
                    "NavBarAdd: ID déjà utilisé: "
                            + title
            );

            return;
        }

        navIds.add(title);

        navIcons.add(
                iconName == null
                        ? ""
                        : iconName
        );
    }

    // =========================================================
    // NAVBAR INITIALIZE
    // =========================================================

    @SimpleFunction(
            description =
                    "Initialise la barre de navigation flottante."
    )
    public void NavBarInitialize(
            final AndroidViewComponent container) {

        if (navBarInitialized) {
            return;
        }

        if (navIds.isEmpty()) {

            OnError(
                    "NavBarInitialize: aucune icône ajoutée."
            );

            return;
        }

        /*
         * IMPORTANT :
         *
         * Screen.Initialize peut être appelé avant que
         * l'arbre graphique Android soit complètement rendu.
         *
         * On utilise post() pour attendre que le DecorView
         * soit prêt.
         */

        final Handler handler =
                new Handler(Looper.getMainLooper());

        handler.post(new Runnable() {

            @Override
            public void run() {

                if (navBarInitialized) {
                    return;
                }

                try {

                    View contentView =
                            activity.getWindow()
                                    .getDecorView()
                                    .findViewById(
                                            android.R.id.content
                                    );

                    if (!(contentView instanceof FrameLayout)) {

                        /*
                         * Si Android n'a pas encore créé
                         * le content root, on réessaie.
                         */

                        handler.postDelayed(
                                this,
                                100
                        );

                        return;
                    }

                    final FrameLayout contentRoot =
                            (FrameLayout) contentView;

                    // =================================================
                    // ROOT NAVIGATION
                    // =================================================

                    navBarRoot =
                            new FrameLayout(activity);

                    navBarRoot.setClipChildren(false);
                    navBarRoot.setClipToPadding(false);

                    FrameLayout.LayoutParams rootParams =
                            new FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                            );

                    contentRoot.addView(
                            navBarRoot,
                            rootParams
                    );

                    // =================================================
                    // BAR
                    // =================================================

                    navBarView =
                            new LinearLayout(activity);

                    navBarView.setOrientation(
                            LinearLayout.HORIZONTAL
                    );

                    navBarView.setGravity(
                            Gravity.CENTER
                    );

                    navBarView.setPadding(
                            dpToPx(4),
                            dpToPx(4),
                            dpToPx(4),
                            dpToPx(4)
                    );

                    GradientDrawable background =
                            new GradientDrawable();

                    background.setColor(Color.WHITE);

                    background.setCornerRadius(
                            dpToPx(30)
                    );

                    navBarView.setBackground(
                            background
                    );

                    if (Build.VERSION.SDK_INT >=
                            Build.VERSION_CODES.LOLLIPOP) {

                        navBarView.setElevation(
                                dpToPx(10)
                        );
                    }

                    // =================================================
                    // ITEMS
                    // =================================================

                    for (int i = 0;
                         i < navIds.size();
                         i++) {

                        final String id =
                                navIds.get(i);

                        final String icon =
                                navIcons.get(i);

                        FrameLayout item =
                                new FrameLayout(activity);

                        LinearLayout.LayoutParams itemParams =
                                new LinearLayout.LayoutParams(
                                        0,
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        1f
                                );

                        itemParams.setMargins(
                                dpToPx(2),
                                0,
                                dpToPx(2),
                                0
                        );

                        item.setLayoutParams(
                                itemParams
                        );

                        // =================================================
                        // CIRCLE
                        // =================================================

                        View circle =
                                new View(activity);

                        GradientDrawable circleBg =
                                new GradientDrawable();

                        circleBg.setShape(
                                GradientDrawable.OVAL
                        );

                        circleBg.setColor(
                                Color.argb(
                                        30,
                                        0,
                                        0,
                                        0
                                )
                        );

                        circle.setBackground(
                                circleBg
                        );

                        circle.setAlpha(0f);

                        FrameLayout.LayoutParams circleParams =
                                new FrameLayout.LayoutParams(
                                        dpToPx(46),
                                        dpToPx(46)
                                );

                        circleParams.gravity =
                                Gravity.CENTER;

                        item.addView(
                                circle,
                                circleParams
                        );

                        // =================================================
                        // ICON
                        // =================================================

                        final ImageView image =
                                new ImageView(activity);

                        image.setScaleType(
                                ImageView.ScaleType.CENTER_INSIDE
                        );

                        FrameLayout.LayoutParams imageParams =
                                new FrameLayout.LayoutParams(
                                        dpToPx(navIconSizeDp),
                                        dpToPx(navIconSizeDp)
                                );

                        imageParams.gravity =
                                Gravity.CENTER;

                        try {

                            DrawableCompatHelper
                                    .setIcon(
                                            image,
                                            form,
                                            icon
                                    );

                        } catch (Exception e) {

                            try {

                                image.setImageDrawable(
                                        MediaUtil
                                                .getBitmapDrawable(
                                                        form,
                                                        icon
                                                )
                                );

                            } catch (Exception ignored) {
                            }
                        }

                        image.setColorFilter(
                                new PorterDuffColorFilter(
                                        Color.rgb(
                                                150,
                                                150,
                                                150
                                        ),
                                        PorterDuff.Mode.SRC_IN
                                )
                        );

                        item.addView(
                                image,
                                imageParams
                        );

                        final View finalCircle =
                                circle;

                        final ImageView finalImage =
                                image;

                        item.setOnClickListener(
                                new View.OnClickListener() {

                                    @Override
                                    public void onClick(
                                            View v) {

                                        selectNavItem(
                                                id,
                                                finalCircle,
                                                finalImage
                                        );
                                    }
                                }
                        );

                        navImages.add(image);
                        navCircles.add(circle);

                        navBarView.addView(item);
                    }

                    // =================================================
                    // POSITION BAR
                    // =================================================

                    FrameLayout.LayoutParams barParams =
                            new FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    dpToPx(64)
                            );

                    barParams.gravity =
                            Gravity.BOTTOM |
                            Gravity.CENTER_HORIZONTAL;

                    barParams.leftMargin =
                            dpToPx(12);

                    barParams.rightMargin =
                            dpToPx(12);

                    barParams.bottomMargin =
                            dpToPx(12);

                    navBarRoot.addView(
                            navBarView,
                            barParams
                    );

                    navBarInitialized = true;

                } catch (Exception e) {

                    e.printStackTrace();

                    OnError(
                            "NavBarInitialize: "
                                    + e.getMessage()
                    );
                }
            }
        });
    }

    // =========================================================
    // NAVBAR SELECT
    // =========================================================

    @SimpleFunction(
            description =
                    "Sélectionne un élément de la barre de navigation par index."
    )
    public void NavBarSelect(
            final int index) {

        if (index < 0 ||
                index >= navIds.size()) {

            OnError(
                    "NavBarSelect: index invalide."
            );

            return;
        }

        runOnUi(
                new Runnable() {

                    @Override
                    public void run() {

                        View circle =
                                navCircles.size() > index
                                        ? navCircles.get(index)
                                        : null;

                        ImageView image =
                                navImages.size() > index
                                        ? navImages.get(index)
                                        : null;

                        selectNavItem(
                                navIds.get(index),
                                circle,
                                image
                        );
                    }
                }
        );
    }

    // =========================================================
    // SELECT NAV ITEM
    // =========================================================

    private void selectNavItem(
            String id,
            View circle,
            ImageView image) {

        if (id == null ||
                circle == null ||
                image == null) {

            return;
        }

        if (id.equals(selectedNavId)) {
            return;
        }

        if (selectedNavId != null) {

            int oldIndex =
                    navIds.indexOf(
                            selectedNavId
                    );

            if (oldIndex >= 0 &&
                    oldIndex < navCircles.size() &&
                    oldIndex < navImages.size()) {

                animateNavItem(
                        navCircles.get(oldIndex),
                        navImages.get(oldIndex),
                        false
                );
            }
        }

        animateNavItem(
                circle,
                image,
                true
        );

        selectedNavId = id;

        OnSelected(id);
    }

    // =========================================================
    // ANIMATION NAV ITEM
    // =========================================================

    private void animateNavItem(
            final View circle,
            final ImageView image,
            boolean selected) {

        final float start =
                circle.getAlpha();

        final float end =
                selected ? 1f : 0f;

        android.animation.ValueAnimator animator =
                android.animation.ValueAnimator
                        .ofFloat(start, end);

        animator.setDuration(220);

        animator.addUpdateListener(
                new android.animation.ValueAnimator
                        .AnimatorUpdateListener() {

                    @Override
                    public void onAnimationUpdate(
                            android.animation.ValueAnimator animation) {

                        float value =
                                (float)
                                        animation
                                                .getAnimatedValue();

                        circle.setAlpha(value);

                        int color =
                                mixColor(
                                        Color.rgb(
                                                150,
                                                150,
                                                150
                                        ),
                                        Color.rgb(
                                                20,
                                                20,
                                                20
                                        ),
                                        value
                                );

                        image.setColorFilter(
                                new PorterDuffColorFilter(
                                        color,
                                        PorterDuff.Mode.SRC_IN
                                )
                        );
                    }
                }
        );

        animator.start();
    }

    // =========================================================
    // MIX COLOR
    // =========================================================

    private int mixColor(
            int c1,
            int c2,
            float ratio) {

        ratio =
                Math.max(
                        0f,
                        Math.min(
                                1f,
                                ratio
                        )
                );

        int r =
                (int)
                        (
                                Color.red(c1)
                                        +
                                        ratio *
                                                (
                                                        Color.red(c2)
                                                                -
                                                                Color.red(c1)
                                                )
                        );

        int g =
                (int)
                        (
                                Color.green(c1)
                                        +
                                        ratio *
                                                (
                                                        Color.green(c2)
                                                                -
                                                                Color.green(c1)
                                                )
                        );

        int b =
                (int)
                        (
                                Color.blue(c1)
                                        +
                                        ratio *
                                                (
                                                        Color.blue(c2)
                                                                -
                                                                Color.blue(c1)
                                                )
                        );

        return Color.rgb(r, g, b);
    }

    // =========================================================
    // NAVBAR ICON SIZE
    // =========================================================

    @SimpleFunction(
            description =
                    "Définit la taille des icônes."
    )
    public void NavBarSetIconSize(
            final int size) {

        if (size <= 0) {
            return;
        }

        navIconSizeDp = size;

        runOnUi(
                new Runnable() {

                    @Override
                    public void run() {

                        for (ImageView image :
                                navImages) {

                            ViewGroup.LayoutParams params =
                                    image.getLayoutParams();

                            if (params != null) {

                                params.width =
                                        dpToPx(
                                                navIconSizeDp
                                        );

                                params.height =
                                        dpToPx(
                                                navIconSizeDp
                                        );

                                image.setLayoutParams(
                                        params
                                );
                            }
                        }
                    }
                }
        );
    }

    // =========================================================
    // CHAT NATIF
    // =========================================================

    @SimpleFunction(
            description =
                    "Ajoute une bulle de chat native avec avatar."
    )
    public void AddChatBubble(
            final AndroidViewComponent chatContainer,
            final String messageText,
            final String timeText,
            final String avatarUrl,
            final boolean isMe,
            final int bubbleColor,
            final int textColor) {

        runOnUi(
                new Runnable() {

                    @Override
                    public void run() {

                        try {

                            ViewGroup target =
                                    getRealLayout(
                                            chatContainer
                                    );

                            if (target == null) {
                                return;
                            }

                            int screenWidth =
                                    activity
                                            .getResources()
                                            .getDisplayMetrics()
                                            .widthPixels;

                            LinearLayout row =
                                    new LinearLayout(
                                            context
                                    );

                            row.setOrientation(
                                    LinearLayout.HORIZONTAL
                            );

                            row.setGravity(
                                    isMe
                                            ? Gravity.END
                                            : Gravity.START
                            );

                            LinearLayout.LayoutParams rowParams =
                                    new LinearLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.WRAP_CONTENT
                                    );

                            rowParams.setMargins(
                                    dpToPx(8),
                                    dpToPx(4),
                                    dpToPx(8),
                                    dpToPx(4)
                            );

                            row.setLayoutParams(
                                    rowParams
                            );

                            int avatarSize =
                                    dpToPx(32);

                            CardView avatarCard =
                                    new CardView(context);

                            LinearLayout.LayoutParams avatarParams =
                                    new LinearLayout.LayoutParams(
                                            avatarSize,
                                            avatarSize
                                    );

                            avatarParams.gravity =
                                    Gravity.CENTER_VERTICAL;

                            avatarParams.setMargins(
                                    dpToPx(6),
                                    0,
                                    dpToPx(6),
                                    0
                            );

                            avatarCard.setLayoutParams(
                                    avatarParams
                            );

                            avatarCard.setRadius(
                                    avatarSize / 2f
                            );

                            avatarCard.setCardElevation(0f);

                            avatarCard.setCardBackgroundColor(
                                    Color.parseColor(
                                            "#E0E0E0"
                                    )
                            );

                            ImageView avatar =
                                    new ImageView(context);

                            avatar.setLayoutParams(
                                    new ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                            );

                            avatar.setScaleType(
                                    ImageView.ScaleType.CENTER_CROP
                            );

                            if (avatarUrl != null &&
                                    !avatarUrl
                                            .trim()
                                            .isEmpty()) {

                                loadImageAsync(
                                        avatar,
                                        avatarUrl
                                );
                            }

                            avatarCard.addView(
                                    avatar
                            );

                            avatarCard.setOnClickListener(
                                    new View.OnClickListener() {

                                        @Override
                                        public void onClick(
                                                View v) {

                                            OnAvatarClick(
                                                    isMe
                                            );
                                        }
                                    }
                            );

                            LinearLayout bubble =
                                    new LinearLayout(
                                            context
                                    );

                            bubble.setOrientation(
                                    LinearLayout.VERTICAL
                            );

                            bubble.setPadding(
                                    dpToPx(16),
                                    dpToPx(10),
                                    dpToPx(16),
                                    dpToPx(10)
                            );

                            GradientDrawable bg =
                                    new GradientDrawable();

                            bg.setColor(
                                    bubbleColor
                            );

                            bg.setCornerRadius(
                                    dpToPx(22)
                            );

                            bubble.setBackground(bg);

                            TextView message =
                                    new TextView(context);

                            message.setText(
                                    messageText
                            );

                            message.setTextColor(
                                    textColor
                            );

                            message.setTextSize(15);

                            message.setMaxWidth(
                                    (int)
                                            (screenWidth *
                                                    0.72f)
                            );

                            if (customTypeface != null) {
                                message.setTypeface(
                                        customTypeface
                                );
                            }

                            bubble.addView(message);

                            if (timeText != null &&
                                    !timeText.isEmpty()) {

                                TextView time =
                                        new TextView(
                                                context
                                        );

                                time.setText(
                                        timeText
                                );

                                time.setTextColor(
                                        Color.argb(
                                                180,
                                                Color.red(
                                                        textColor
                                                ),
                                                Color.green(
                                                        textColor
                                                ),
                                                Color.blue(
                                                        textColor
                                                )
                                        )
                                );

                                time.setTextSize(10);

                                LinearLayout.LayoutParams timeParams =
                                        new LinearLayout.LayoutParams(
                                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                                ViewGroup.LayoutParams.WRAP_CONTENT
                                        );

                                timeParams.gravity =
                                        Gravity.END;

                                time.setLayoutParams(
                                        timeParams
                                );

                                bubble.addView(time);
                            }

                            if (isMe) {

                                row.addView(
                                        bubble
                                );

                                row.addView(
                                        avatarCard
                                );

                            } else {

                                row.addView(
                                        avatarCard
                                );

                                row.addView(
                                        bubble
                                );
                            }

                            target.addView(row);

                            ScrollToBottom(
                                    chatContainer
                            );

                        } catch (Exception e) {

                            e.printStackTrace();

                            OnError(
                                    "AddChatBubble Error: "
                                            + e.getMessage()
                            );
                        }
                    }
                }
        );
    }

    // =========================================================
    // SCROLL TO BOTTOM
    // =========================================================

    @SimpleFunction(
            description =
                    "Fait défiler une vue vers le bas."
    )
    public void ScrollToBottom(
            final AndroidViewComponent scrollComponent) {

        if (scrollComponent == null) {
            return;
        }

        final View view =
                scrollComponent.getView();

        if (view instanceof ScrollView) {

            final ScrollView scrollView =
                    (ScrollView) view;

            scrollView.post(
                    new Runnable() {

                        @Override
                        public void run() {

                            scrollView.fullScroll(
                                    View.FOCUS_DOWN
                            );
                        }
                    }
            );

        } else if (view instanceof ViewGroup) {

            final ViewGroup group =
                    (ViewGroup) view;

            group.post(
                    new Runnable() {

                        @Override
                        public void run() {

                            group.requestFocus();
                        }
                    }
            );
        }
    }

    // =========================================================
    // ATTACH FLOATING INPUT
    // =========================================================

    @SimpleFunction(
            description =
                    "Attache la zone de saisie au-dessus du clavier avec hauteur maximale."
    )
    public void AttachFloatingInputWithDynamicHeight(
            final AndroidViewComponent inputContainer,
            final String editTextComponent,
            final int maxHeightPx) {

        if (inputContainer == null) {
            return;
        }

        final View container =
                inputContainer.getView();

        if (container == null) {
            return;
        }

        /*
         * editTextComponent est maintenant un String.
         *
         * Tu peux donc directement utiliser :
         *
         * TextBox.Text
         *
         * dans Kodular/App Inventor.
         *
         * La valeur n'a pas besoin d'être utilisée ici
         * pour déplacer le container.
         */

        final String currentText =
                editTextComponent == null
                        ? ""
                        : editTextComponent;

        final View root =
                activity
                        .getWindow()
                        .getDecorView()
                        .getRootView();

        root.getViewTreeObserver()
                .addOnGlobalLayoutListener(
                        new ViewTreeObserver
                                .OnGlobalLayoutListener() {

                            @Override
                            public void onGlobalLayout() {

                                try {

                                    Rect rect =
                                            new Rect();

                                    root.getWindowVisibleDisplayFrame(
                                            rect
                                    );

                                    int screenHeight =
                                            root.getRootView()
                                                    .getHeight();

                                    int keyboardHeight =
                                            screenHeight
                                                    -
                                                    rect.bottom;

                                    /*
                                     * Clavier détecté
                                     */

                                    if (keyboardHeight >
                                            screenHeight *
                                                    0.15f) {

                                        int translation =
                                                keyboardHeight;

                                        if (maxHeightPx > 0) {

                                            translation =
                                                    Math.min(
                                                            translation,
                                                            maxHeightPx
                                                    );
                                        }

                                        container.setTranslationY(
                                                -translation
                                        );

                                    } else {

                                        container.setTranslationY(
                                                0
                                        );
                                    }

                                } catch (Exception e) {

                                    e.printStackTrace();
                                }
                            }
                        }
                );
    }

    // =========================================================
    // PRODUITS
    // =========================================================

    @SimpleFunction(
            description =
                    "Génère une grille 2x2 de produits depuis un JSON."
    )
    public void BuildProductGridFromJson(
            final AndroidViewComponent scrollContainer,
            final String jsonData) {

        AsynchUtil.runAsynchronously(
                new Runnable() {

                    @Override
                    public void run() {

                        try {

                            final JSONArray array =
                                    new JSONArray(
                                            jsonData
                                    );

                            final int screenWidth =
                                    activity
                                            .getResources()
                                            .getDisplayMetrics()
                                            .widthPixels;

                            runOnUi(
                                    new Runnable() {

                                        @Override
                                        public void run() {

                                            try {

                                                ViewGroup target =
                                                        getRealLayout(
                                                                scrollContainer
                                                        );

                                                if (target == null) {
                                                    return;
                                                }

                                                target.removeAllViews();

                                                int cardWidth =
                                                        (int)
                                                                (
                                                                        screenWidth *
                                                                                0.43f
                                                                );

                                                int cardHeight =
                                                        dpToPx(220);

                                                LinearLayout row =
                                                        null;

                                                for (
                                                        int i = 0;
                                                        i < array.length();
                                                        i++
                                                ) {

                                                    JSONObject item =
                                                            array.getJSONObject(
                                                                    i
                                                            );

                                                    final String uid =
                                                            item.optString(
                                                                    "uid",
                                                                    String.valueOf(
                                                                            i
                                                                    )
                                                            );

                                                    String image =
                                                            item.optString(
                                                                    "image",
                                                                    ""
                                                            );

                                                    String title =
                                                            item.optString(
                                                                    "title",
                                                                    ""
                                                            );

                                                    String price =
                                                            item.optString(
                                                                    "price",
                                                                    ""
                                                            );

                                                    if (i % 2 == 0) {

                                                        row =
                                                                new LinearLayout(
                                                                        context
                                                                );

                                                        row.setOrientation(
                                                                LinearLayout.HORIZONTAL
                                                        );

                                                        row.setGravity(
                                                                Gravity.CENTER
                                                        );

                                                        row.setLayoutParams(
                                                                new LinearLayout.LayoutParams(
                                                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                                                        ViewGroup.LayoutParams.WRAP_CONTENT
                                                                )
                                                        );

                                                        target.addView(
                                                                row
                                                        );
                                                    }

                                                    CardView card =
                                                            new CardView(
                                                                    context
                                                            );

                                                    LinearLayout.LayoutParams cardParams =
                                                            new LinearLayout.LayoutParams(
                                                                    cardWidth,
                                                                    cardHeight
                                                            );

                                                    cardParams.setMargins(
                                                            dpToPx(6),
                                                            dpToPx(6),
                                                            dpToPx(6),
                                                            dpToPx(6)
                                                    );

                                                    card.setLayoutParams(
                                                            cardParams
                                                    );

                                                    card.setRadius(
                                                            dpToPx(16)
                                                    );

                                                    card.setCardBackgroundColor(
                                                            Color.WHITE
                                                    );

                                                    card.setCardElevation(
                                                            dpToPx(2)
                                                    );

                                                    LinearLayout inner =
                                                            new LinearLayout(
                                                                    context
                                                            );

                                                    inner.setOrientation(
                                                            LinearLayout.VERTICAL
                                                    );

                                                    ImageView imageView =
                                                            new ImageView(
                                                                    context
                                                            );

                                                    imageView.setScaleType(
                                                            ImageView.ScaleType.CENTER_CROP
                                                    );

                                                    imageView.setLayoutParams(
                                                            new LinearLayout.LayoutParams(
                                                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                                                    dpToPx(120)
                                                            )
                                                    );

                                                    loadImageAsync(
                                                            imageView,
                                                            image
                                                    );

                                                    inner.addView(
                                                            imageView
                                                    );

                                                    TextView titleView =
                                                            new TextView(
                                                                    context
                                                            );

                                                    titleView.setText(
                                                            title
                                                    );

                                                    titleView.setTextColor(
                                                            Color.BLACK
                                                    );

                                                    titleView.setTextSize(
                                                            13
                                                    );

                                                    titleView.setMaxLines(
                                                            2
                                                    );

                                                    titleView.setPadding(
                                                            dpToPx(10),
                                                            dpToPx(6),
                                                            dpToPx(10),
                                                            0
                                                    );

                                                    if (customTypeface != null) {

                                                        titleView.setTypeface(
                                                                customTypeface
                                                        );
                                                    }

                                                    inner.addView(
                                                            titleView
                                                    );

                                                    TextView priceView =
                                                            new TextView(
                                                                    context
                                                            );

                                                    priceView.setText(
                                                            price
                                                    );

                                                    priceView.setTextColor(
                                                            Color.BLACK
                                                    );

                                                    priceView.setTextSize(
                                                            14
                                                    );

                                                    priceView.setPadding(
                                                            dpToPx(10),
                                                            dpToPx(2),
                                                            dpToPx(10),
                                                            dpToPx(8)
                                                    );

                                                    if (customTypeface != null) {

                                                        priceView.setTypeface(
                                                                customTypeface,
                                                                Typeface.BOLD
                                                        );
                                                    }

                                                    inner.addView(
                                                            priceView
                                                    );

                                                    card.addView(
                                                            inner
                                                    );

                                                    card.setOnClickListener(
                                                            new View.OnClickListener() {

                                                                @Override
                                                                public void onClick(
                                                                        View v) {

                                                                    OnProductCardClick(
                                                                            uid
                                                                    );
                                                                }
                                                            }
                                                    );

                                                    if (row != null) {

                                                        row.addView(
                                                                card
                                                        );
                                                    }
                                                }

                                            } catch (Exception e) {

                                                e.printStackTrace();

                                                OnError(
                                                        "BuildProductGrid Error: "
                                                                + e.getMessage()
                                                );
                                            }
                                        }
                                    }
                            );

                        } catch (Exception e) {

                            e.printStackTrace();

                            OnError(
                                    "JSON Error: "
                                            + e.getMessage()
                            );
                        }
                    }
                }
        );
    }

    // =========================================================
    // CATEGORIES
    // =========================================================

    @SimpleFunction(
            description =
                    "Génère la liste dynamique des catégories."
    )
    public void BuildCategoryListFromJson(
            final AndroidViewComponent listContainer,
            final String categoriesJson) {

        AsynchUtil.runAsynchronously(
                new Runnable() {

                    @Override
                    public void run() {

                        try {

                            final JSONArray mainArray =
                                    new JSONArray(
                                            categoriesJson
                                    );

                            runOnUi(
                                    new Runnable() {

                                        @Override
                                        public void run() {

                                            try {

                                                ViewGroup target =
                                                        getRealLayout(
                                                                listContainer
                                                        );

                                                if (target == null) {
                                                    return;
                                                }

                                                target.removeAllViews();

                                                RadioGroup group =
                                                        new RadioGroup(
                                                                activity
                                                        );

                                                group.setOrientation(
                                                        LinearLayout.VERTICAL
                                                );

                                                ColorStateList radioColors =
                                                        ColorStateList.valueOf(
                                                                radioButtonColor
                                                        );

                                                for (
                                                        int i = 0;
                                                        i < mainArray.length();
                                                        i++
                                                ) {

                                                    JSONObject category =
                                                            mainArray
                                                                    .getJSONObject(
                                                                            i
                                                                    );

                                                    String categoryName =
                                                            category.optString(
                                                                    "title",
                                                                    ""
                                                            );

                                                    JSONArray subCategories =
                                                            category.optJSONArray(
                                                                    "subcategories"
                                                            );

                                                    TextView header =
                                                            new TextView(
                                                                    activity
                                                            );

                                                    header.setText(
                                                            ">  "
                                                                    + categoryName
                                                    );

                                                    header.setTextColor(
                                                            Color.parseColor(
                                                                    "#E91A1A1B"
                                                            )
                                                    );

                                                    header.setTextSize(
                                                            18
                                                    );

                                                    header.setTypeface(
                                                            customTypeface,
                                                            Typeface.BOLD
                                                    );

                                                    header.setPadding(
                                                            0,
                                                            dpToPx(16),
                                                            0,
                                                            dpToPx(8)
                                                    );

                                                    group.addView(
                                                            header
                                                    );

                                                    if (subCategories != null) {

                                                        for (
                                                                int j = 0;
                                                                j < subCategories.length();
                                                                j++
                                                        ) {

                                                            JSONObject sub =
                                                                    subCategories
                                                                            .getJSONObject(
                                                                                    j
                                                                            );

                                                            final String id =
                                                                    sub.optString(
                                                                            "id",
                                                                            ""
                                                                    );

                                                            final String title =
                                                                    sub.optString(
                                                                            "title",
                                                                            ""
                                                                    );

                                                            RadioButton button =
                                                                    new RadioButton(
                                                                            activity
                                                                    );

                                                            button.setId(
                                                                    View.generateViewId()
                                                            );

                                                            button.setText(
                                                                    title
                                                            );

                                                            button.setTextColor(
                                                                    Color.parseColor(
                                                                            "#C01A1A1B"
                                                                    )
                                                            );

                                                            button.setTextSize(
                                                                    13
                                                            );

                                                            if (Build.VERSION.SDK_INT >=
                                                                    Build.VERSION_CODES.LOLLIPOP) {

                                                                button.setButtonTintList(
                                                                        radioColors
                                                                );
                                                            }

                                                            if (customTypeface != null) {

                                                                button.setTypeface(
                                                                        customTypeface
                                                                );
                                                            }

                                                            button.setPadding(
                                                                    dpToPx(8),
                                                                    dpToPx(12),
                                                                    dpToPx(8),
                                                                    dpToPx(12)
                                                            );

                                                            button.setOnClickListener(
                                                                    new View.OnClickListener() {

                                                                        @Override
                                                                        public void onClick(
                                                                                View v) {

                                                                            OnCategorySelected(
                                                                                    id,
                                                                                    title
                                                                            );
                                                                        }
                                                                    }
                                                            );

                                                            group.addView(
                                                                    button
                                                            );

                                                            View divider =
                                                                    new View(
                                                                            activity
                                                                    );

                                                            divider.setLayoutParams(
                                                                    new LinearLayout.LayoutParams(
                                                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                                                            dpToPx(1)
                                                                    )
                                                            );

                                                            divider.setBackgroundColor(
                                                                    Color.parseColor(
                                                                            "#F0F0F0"
                                                                    )
                                                            );

                                                            group.addView(
                                                                    divider
                                                            );
                                                        }
                                                    }
                                                }

                                                target.addView(
                                                        group
                                                );

                                            } catch (Exception e) {

                                                e.printStackTrace();
                                            }
                                        }
                                    }
                            );

                        } catch (Exception e) {

                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    // =========================================================
    // EVENTS
    // =========================================================

    @SimpleEvent(
            description =
                    "Déclenché lors de la sélection d'un onglet."
    )
    public void OnSelected(String id) {

        EventDispatcher.dispatchEvent(
                this,
                "OnSelected",
                id
        );
    }

    @SimpleEvent(
            description =
                    "Déclenché lorsqu'une carte produit est sélectionnée."
    )
    public void OnProductCardClick(
            String productUid) {

        EventDispatcher.dispatchEvent(
                this,
                "OnProductCardClick",
                productUid
        );
    }

    @SimpleEvent(
            description =
                    "Déclenché lors du clic sur l'avatar."
    )
    public void OnAvatarClick(
            boolean isMe) {

        EventDispatcher.dispatchEvent(
                this,
                "OnAvatarClick",
                isMe
        );
    }

    @SimpleEvent(
            description =
                    "Déclenché lorsqu'une catégorie est sélectionnée."
    )
    public void OnCategorySelected(
            String categoryId,
            String categoryTitle) {

        EventDispatcher.dispatchEvent(
                this,
                "OnCategorySelected",
                categoryId,
                categoryTitle
        );
    }

    @SimpleEvent(
            description =
                    "Erreur de l'extension."
    )
    public void OnError(
            String message) {

        EventDispatcher.dispatchEvent(
                this,
                "OnError",
                message
        );
    }

    // =========================================================
    // ACTIVITY RESULT
    // =========================================================

    @Override
    public void resultReturned(
            int receivedRequestCode,
            int resultCode,
            Intent data) {
    }

    // =========================================================
    // DRAWABLE HELPER
    // =========================================================

    private static class DrawableCompatHelper {

        static void setIcon(
                ImageView image,
                Form form,
                String path)
                throws Exception {

            image.setImageDrawable(
                    MediaUtil.getBitmapDrawable(
                            form,
                            path
                    )
            );
        }
    }
                            }
