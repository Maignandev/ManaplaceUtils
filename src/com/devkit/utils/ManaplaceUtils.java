package com.devkit.utils;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
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

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.AndroidViewComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.Form;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.MediaUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@DesignerComponent(
        version = 6,
        description = "ManaplaceUtils - Extension utilitaire complète.",
        category = ComponentCategory.EXTENSION,
        nonVisible = true
)
@SimpleObject(external = true)
@UsesPermissions(
        permissionNames =
                "android.permission.INTERNET," +
                "android.permission.READ_EXTERNAL_STORAGE," +
                "android.permission.READ_MEDIA_IMAGES"
)
public class ManaplaceUtils extends AndroidNonvisibleComponent
        implements Form.ActivityResultListener {

    private final Context context;
    private final Activity activity;
    private final Form form;
    private final int requestCode;

    private Typeface customTypeface = Typeface.DEFAULT;

    private int radioButtonColor =
            Color.rgb(192, 26, 27);

    private AlertDialog currentAlphaDialog;

    // ============================================================
    // NAVIGATION
    // ============================================================

    private boolean navBarInitialized = false;

    private int navIconSizeDp = 26;

    private final List<String> navIds =
            new ArrayList<>();

    private final List<String> navIcons =
            new ArrayList<>();

    private final List<ImageView> navImages =
            new ArrayList<>();

    private final List<View> navCircles =
            new ArrayList<>();

    private String selectedNavId = null;

    private FrameLayout navBarRoot = null;

    private LinearLayout navBarView = null;

    // ============================================================
    // CONSTRUCTEUR
    // ============================================================

    public ManaplaceUtils(ComponentContainer container) {
        super(container.$form());

        context = container.$context();

        activity = (Activity) container.$context();

        form = container.$form();

        requestCode =
                form.registerForActivityResult(this);
    }

    // ============================================================
    // UTILITAIRES
    // ============================================================

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

    // ============================================================
    // POLICE
    // ============================================================

    @SimpleFunction(
            description = "Charge une police TTF ou OTF."
    )
    public void LoadCustomFont(String fontPath) {

        try {

            if (fontPath == null ||
                    fontPath.trim().isEmpty()) {

                customTypeface =
                        Typeface.DEFAULT;

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

            customTypeface =
                    Typeface.DEFAULT;

            e.printStackTrace();
        }
    }

    // ============================================================
    // RADIO BUTTON
    // ============================================================

    @SimpleFunction(
            description = "Définit la couleur des boutons radio."
    )
    public void SetRadioButtonColor(int color) {
        radioButtonColor = color;
    }

    // ============================================================
    // IMAGE ASYNCHRONE
    // ============================================================

    private void loadImageAsync(
            final ImageView imageView,
            final String imagePath) {

        if (imageView == null ||
                imagePath == null ||
                imagePath.trim().isEmpty()) {
            return;
        }

        AsynchUtil.runAsynchronously(
                new Runnable() {

                    @Override
                    public void run() {

                        Bitmap bitmap = null;

                        InputStream input = null;

                        HttpURLConnection connection = null;

                        try {

                            if (imagePath.startsWith(
                                    "http://"
                            ) ||
                                    imagePath.startsWith(
                                            "https://"
                                    )) {

                                URL url =
                                        new URL(imagePath);

                                connection =
                                        (HttpURLConnection)
                                                url.openConnection();

                                connection.setConnectTimeout(
                                        15000
                                );

                                connection.setReadTimeout(
                                        15000
                                );

                                connection.setDoInput(
                                        true
                                );

                                connection.connect();

                                input =
                                        connection
                                                .getInputStream();

                                bitmap =
                                        BitmapFactory
                                                .decodeStream(
                                                        input
                                                );

                            } else if (
                                    imagePath.startsWith(
                                            "content://"
                                    )) {

                                input =
                                        context
                                                .getContentResolver()
                                                .openInputStream(
                                                        Uri.parse(
                                                                imagePath
                                                        )
                                                );

                                if (input != null) {

                                    bitmap =
                                            BitmapFactory
                                                    .decodeStream(
                                                            input
                                                    );
                                }

                            } else {

                                try {

                                    input =
                                            context
                                                    .getAssets()
                                                    .open(
                                                            imagePath
                                                    );

                                    bitmap =
                                            BitmapFactory
                                                    .decodeStream(
                                                            input
                                                    );

                                } catch (Exception assetError) {

                                    try {

                                        bitmap =
                                                MediaUtil
                                                        .getBitmapDrawable(
                                                                form,
                                                                imagePath
                                                        )
                                                        .getBitmap();

                                    } catch (Exception mediaError) {

                                        File file =
                                                new File(
                                                        imagePath
                                                );

                                        if (file.exists()) {

                                            input =
                                                    new FileInputStream(
                                                            file
                                                    );

                                            bitmap =
                                                    BitmapFactory
                                                            .decodeStream(
                                                                    input
                                                            );
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

                            if (connection != null) {
                                connection.disconnect();
                            }
                        }

                        final Bitmap finalBitmap =
                                bitmap;

                        if (finalBitmap != null) {

                            activity.runOnUiThread(
                                    new Runnable() {

                                        @Override
                                        public void run() {

                                            try {

                                                if (imageView
                                                        .getWindowToken()
                                                        != null) {

                                                    imageView
                                                            .setImageBitmap(
                                                                    finalBitmap
                                                            );
                                                }

                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                            );
                        }
                    }
                }
        );
    }

    // ============================================================
    // NAVBAR - AJOUT
    // ============================================================

    @SimpleFunction(
            description = "Ajoute un élément à la barre de navigation."
    )
    public void NavBarAdd(
            AndroidViewComponent container,
            String title,
            String iconName) {

        if (title == null ||
                title.trim().isEmpty()) {

            OnError(
                    "NavBarAdd: ID vide."
            );

            return;
        }

        if (navIds.contains(title)) {

            OnError(
                    "NavBarAdd: ID déjà utilisé: " +
                            title
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

    // ============================================================
    // NAVBAR - INITIALISATION
    // ============================================================

    @SimpleFunction(
            description = "Initialise la barre de navigation flottante."
    )
    public void NavBarInitialize(
            AndroidViewComponent container) {

        if (navBarInitialized) {
            return;
        }

        if (navIds.isEmpty()) {

            OnError(
                    "NavBarInitialize: aucune icône ajoutée."
            );

            return;
        }

        runOnUi(
                new Runnable() {

                    @Override
                    public void run() {

                        try {

                            View content =
                                    activity.findViewById(
                                            android.R.id.content
                                    );

                            if (!(content instanceof FrameLayout)) {

                                OnError(
                                        "NavBarInitialize: " +
                                                "le root content n'est pas un FrameLayout."
                                );

                                return;
                            }

                            navBarRoot =
                                    (FrameLayout) content;

                            navBarView =
                                    new LinearLayout(
                                            activity
                                    );

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

                            background.setColor(
                                    Color.WHITE
                            );

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

                            for (
                                    int i = 0;
                                    i < navIds.size();
                                    i++
                            ) {

                                final String id =
                                        navIds.get(i);

                                final String icon =
                                        navIcons.get(i);

                                FrameLayout item =
                                        new FrameLayout(
                                                activity
                                        );

                                LinearLayout.LayoutParams
                                        itemParams =
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

                                View circle =
                                        new View(activity);

                                GradientDrawable
                                        circleBackground =
                                        new GradientDrawable();

                                circleBackground.setShape(
                                        GradientDrawable.OVAL
                                );

                                circleBackground.setColor(
                                        Color.argb(
                                                30,
                                                0,
                                                0,
                                                0
                                        )
                                );

                                circle.setBackground(
                                        circleBackground
                                );

                                circle.setAlpha(0f);

                                FrameLayout.LayoutParams
                                        circleParams =
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

                                ImageView image =
                                        new ImageView(
                                                activity
                                        );

                                image.setScaleType(
                                        ImageView.ScaleType
                                                .CENTER_INSIDE
                                );

                                FrameLayout.LayoutParams
                                        imageParams =
                                        new FrameLayout.LayoutParams(
                                                dpToPx(
                                                        navIconSizeDp
                                                ),
                                                dpToPx(
                                                        navIconSizeDp
                                                )
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
                                                    View view) {

                                                selectNavItem(
                                                        id,
                                                        finalCircle,
                                                        finalImage
                                                );
                                            }
                                        }
                                );

                                navImages.add(
                                        image
                                );

                                navCircles.add(
                                        circle
                                );

                                navBarView.addView(
                                        item
                                );
                            }

                            FrameLayout.LayoutParams
                                    barParams =
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

                            navBarInitialized =
                                    true;

                        } catch (Exception e) {

                            e.printStackTrace();

                            OnError(
                                    "NavBarInitialize: " +
                                            e.getMessage()
                            );
                        }
                    }
                }
        );
    }

    // ============================================================
    // NAVBAR - SELECTION
    // ============================================================

    @SimpleFunction(
            description = "Sélectionne un élément de la navigation."
    )
    public void NavBarSelect(int index) {

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
                                index <
                                        navCircles.size()
                                        ? navCircles.get(index)
                                        : null;

                        ImageView image =
                                index <
                                        navImages.size()
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

        selectedNavId =
                id;

        OnSelected(id);
    }

    private void animateNavItem(
            final View circle,
            final ImageView image,
            boolean selected) {

        final float start =
                circle.getAlpha();

        final float end =
                selected
                        ? 1f
                        : 0f;

        android.animation.ValueAnimator animator =
                android.animation.ValueAnimator.ofFloat(
                        start,
                        end
                );

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

                        circle.setAlpha(
                                value
                        );

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

    private int mixColor(
            int color1,
            int color2,
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
                                Color.red(color1)
                                        +
                                        ratio *
                                                (
                                                        Color.red(color2)
                                                                -
                                                                Color.red(color1)
                                                )
                        );

        int g =
                (int)
                        (
                                Color.green(color1)
                                        +
                                        ratio *
                                                (
                                                        Color.green(color2)
                                                                -
                                                                Color.green(color1)
                                                )
                        );

        int b =
                (int)
                        (
                                Color.blue(color1)
                                        +
                                        ratio *
                                                (
                                                        Color.blue(color2)
                                                                -
                                                                Color.blue(color1)
                                                )
                        );

        return Color.rgb(
                r,
                g,
                b
        );
    }

    @SimpleFunction(
            description = "Définit la taille des icônes de navigation."
    )
    public void NavBarSetIconSize(int size) {

        if (size <= 0) {
            return;
        }

        navIconSizeDp =
                size;

        runOnUi(
                new Runnable() {

                    @Override
                    public void run() {

                        for (
                                ImageView image :
                                navImages
                        ) {

                            ViewGroup.LayoutParams
                                    params =
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

    // ============================================================
    // CHAT
    // ============================================================

    @SimpleFunction(
            description = "Ajoute une bulle de chat native."
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

                            LinearLayout.LayoutParams
                                    rowParams =
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
                                    new CardView(
                                            context
                                    );

                            LinearLayout.LayoutParams
                                    avatarParams =
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

                            avatarCard.setCardElevation(
                                    0f
                            );

                            avatarCard.setCardBackgroundColor(
                                    Color.LTGRAY
                            );

                            ImageView avatar =
                                    new ImageView(
                                            context
                                    );

                            avatar.setLayoutParams(
                                    new ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                            );

                            avatar.setScaleType(
                                    ImageView.ScaleType.CENTER_CROP
                            );

                            loadImageAsync(
                                    avatar,
                                    avatarUrl
                            );

                            avatarCard.addView(
                                    avatar
                            );

                            avatarCard.setOnClickListener(
                                    new View.OnClickListener() {

                                        @Override
                                        public void onClick(
                                                View view) {

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

                            GradientDrawable
                                    bubbleBackground =
                                    new GradientDrawable();

                            bubbleBackground.setColor(
                                    bubbleColor
                            );

                            bubbleBackground.setCornerRadius(
                                    dpToPx(22)
                            );

                            bubble.setBackground(
                                    bubbleBackground
                            );

                            TextView message =
                                    new TextView(
                                            context
                                    );

                            message.setText(
                                    messageText == null
                                            ? ""
                                            : messageText
                            );

                            message.setTextColor(
                                    textColor
                            );

                            message.setTextSize(
                                    15
                            );

                            message.setMaxWidth(
                                    (int)
                                            (
                                                    screenWidth *
                                                            0.72f
                                            )
                            );

                            message.setTypeface(
                                    customTypeface
                            );

                            bubble.addView(
                                    message
                            );

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

                                time.setTextSize(
                                        10
                                );

                                LinearLayout.LayoutParams
                                        timeParams =
                                        new LinearLayout.LayoutParams(
                                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                                ViewGroup.LayoutParams.WRAP_CONTENT
                                        );

                                timeParams.gravity =
                                        Gravity.END;

                                time.setLayoutParams(
                                        timeParams
                                );

                                bubble.addView(
                                        time
                                );
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

                            target.addView(
                                    row
                            );

                            ScrollToBottom(
                                    chatContainer
                            );

                        } catch (Exception e) {

                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    // ============================================================
    // CHAT TEMPLATE
    // ============================================================

    @SimpleFunction(
            description = "Ajoute un message avec un template Kodular."
    )
    public void AddChatMessageFromTemplate(
            final AndroidViewComponent chatContainer,
            final AndroidViewComponent templateBubbleCard,
            final String messageText,
            final String timestamp,
            final String senderUid,
            final boolean isMe,
            final int bubbleColor,
            final String avatarUrl) {

        runOnUi(
                new Runnable() {

                    @Override
                    public void run() {

                        try {

                            ViewGroup parent =
                                    getRealLayout(
                                            chatContainer
                                    );

                            if (templateBubbleCard == null) {
                                return;
                            }

                            View template =
                                    templateBubbleCard.getView();

                            if (parent == null ||
                                    template == null) {
                                return;
                            }

                            if (template.getParent()
                                    instanceof ViewGroup) {

                                ((ViewGroup)
                                        template.getParent())
                                        .removeView(
                                                template
                                        );
                            }

                            template.setBackgroundColor(
                                    bubbleColor
                            );

                            LinearLayout.LayoutParams
                                    params =
                                    new LinearLayout.LayoutParams(
                                            ViewGroup.LayoutParams.WRAP_CONTENT,
                                            ViewGroup.LayoutParams.WRAP_CONTENT
                                    );

                            params.gravity =
                                    isMe
                                            ? Gravity.END
                                            : Gravity.START;

                            params.setMargins(
                                    dpToPx(12),
                                    dpToPx(8),
                                    dpToPx(12),
                                    dpToPx(8)
                            );

                            template.setLayoutParams(
                                    params
                            );

                            template.setTag(
                                    senderUid
                            );

                            template.setOnClickListener(
                                    new View.OnClickListener() {

                                        @Override
                                        public void onClick(
                                                View view) {

                                            OnUserAvatarClick(
                                                    senderUid,
                                                    isMe
                                                            ? "me"
                                                            : "user"
                                            );
                                        }
                                    }
                            );

                            parent.addView(
                                    template
                            );

                            ScrollToBottom(
                                    chatContainer
                            );

                        } catch (Exception e) {

                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    // ============================================================
    // SCROLL
    // ============================================================

    @SimpleFunction(
            description = "Fait défiler automatiquement vers le bas."
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

    // ============================================================
    // INPUT / CLAVIER
    // ============================================================

    @SimpleFunction(
            description = "Place un conteneur au-dessus du clavier."
    )
    public void AttachFloatingInputWithDynamicHeight(
            final Object inputContainer,
            final Object editTextComponent,
            final int maxHeightPx) {

        if (!(inputContainer instanceof
                AndroidViewComponent)) {
            return;
        }

        final View container =
                ((AndroidViewComponent)
                        inputContainer)
                        .getView();

        if (container == null) {
            return;
        }

        final View root =
                activity.getWindow()
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
                                            screenHeight -
                                                    rect.bottom;

                                    if (keyboardHeight >
                                            screenHeight * 0.15f) {

                                        int translation =
                                                keyboardHeight;

                                        if (maxHeightPx > 0) {

                                            translation =
                                                    Math.min(
                                                            translation,
                                                            maxHeightPx
                                                    );
                                        }

                                        container
                                                .setTranslationY(
                                                        -translation
                                                );

                                    } else {

                                        container
                                                .setTranslationY(
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

    // ============================================================
    // PRODUITS JSON
    // ============================================================

    @SimpleFunction(
            description = "Génère une grille 2 colonnes depuis un JSON."
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

                            final int screenHeight =
                                    activity
                                            .getResources()
                                            .getDisplayMetrics()
                                            .heightPixels;

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
                                                                        screenWidth
                                                                                *
                                                                                0.44f
                                                                );

                                                int cardHeight =
                                                        (int)
                                                                (
                                                                        screenHeight
                                                                                *
                                                                                0.28f
                                                                );

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

                                                    LinearLayout.LayoutParams
                                                            cardParams =
                                                            new LinearLayout.LayoutParams(
                                                                    cardWidth,
                                                                    cardHeight
                                                            );

                                                    cardParams.setMargins(
                                                            dpToPx(5),
                                                            dpToPx(8),
                                                            dpToPx(5),
                                                            dpToPx(8)
                                                    );

                                                    card.setLayoutParams(
                                                            cardParams
                                                    );

                                                    card.setRadius(
                                                            dpToPx(20)
                                                    );

                                                    card.setCardBackgroundColor(
                                                            Color.WHITE
                                                    );

                                                    card.setCardElevation(
                                                            2f
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
                                                            ImageView.ScaleType
                                                                    .CENTER_CROP
                                                    );

                                                    imageView.setLayoutParams(
                                                            new LinearLayout.LayoutParams(
                                                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                                                    0,
                                                                    1f
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
                                                            dpToPx(14),
                                                            dpToPx(8),
                                                            dpToPx(14),
                                                            0
                                                    );

                                                    titleView.setTypeface(
                                                            customTypeface
                                                    );

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
                                                            dpToPx(14),
                                                            dpToPx(2),
                                                            dpToPx(14),
                                                            dpToPx(12)
                                                    );

                                                    priceView.setTypeface(
                                                            customTypeface,
                                                            Typeface.BOLD
                                                    );

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
                                                                        View view) {

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

    // ============================================================
    // CATÉGORIES JSON
    // ============================================================

    @SimpleFunction(
            description = "Génère une liste de catégories depuis un JSON."
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

                                                ColorStateList colors =
                                                        ColorStateList
                                                                .valueOf(
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

                                                    TextView header =
                                                            new TextView(
                                                                    activity
                                                            );

                                                    header.setText(
                                                            ">  " +
                                                                    categoryName
                                                    );

                                                    header.setTextColor(
                                                            Color.rgb(
                                                                    26,
                                                                    26,
                                                                    27
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

                                                    JSONArray
                                                            subCategories =
                                                            category
                                                                    .optJSONArray(
                                                                            "subcategories"
                                                                    );

                                                    if (subCategories ==
                                                            null) {
                                                        continue;
                                                    }

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
                                                                Color.rgb(
                                                                        26,
                                                                        26,
                                                                        27
                                                                )
                                                        );

                                                        button.setTextSize(
                                                                13
                                                        );

                                                        if (Build.VERSION.SDK_INT >=
                                                                Build.VERSION_CODES.LOLLIPOP) {

                                                            button.setButtonTintList(
                                                                    colors
                                                            );
                                                        }

                                                        button.setTypeface(
                                                                customTypeface
                                                        );

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
                                                                            View view) {

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
                                                                Color.rgb(
                                                                        240,
                                                                        240,
                                                                        240
                                                                )
                                                        );

                                                        group.addView(
                                                                divider
                                                        );
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

    // ============================================================
    // GRADIENT
    // ============================================================

    @SimpleFunction(
            description = "Applique un arrière-plan dégradé."
    )
    public void SetGradientBackground(
            AndroidViewComponent component,
            int startColor,
            int endColor,
            int orientation,
            float cornerRadius) {

        try {

            final ViewGroup target =
                    getRealLayout(
                            component
                    );

            if (target == null) {
                return;
            }

            GradientDrawable.Orientation
                    gradientOrientation =
                    GradientDrawable.Orientation
                            .TOP_BOTTOM;

            switch (orientation) {

                case 1:
                    gradientOrientation =
                            GradientDrawable.Orientation
                                    .LEFT_RIGHT;
                    break;

                case 2:
                    gradientOrientation =
                            GradientDrawable.Orientation
                                    .TL_BR;
                    break;

                case 3:
                    gradientOrientation =
                            GradientDrawable.Orientation
                                    .BL_TR;
                    break;

                default:
                    break;
            }

            final GradientDrawable drawable =
                    new GradientDrawable(
                            gradientOrientation,
                            new int[]{
                                    startColor,
                                    endColor
                            }
                    );

            drawable.setCornerRadius(
                    dpToPx(
                            Math.max(
                                    0,
                                    (int) cornerRadius
                            )
                    )
            );

            runOnUi(
                    new Runnable() {

                        @Override
                        public void run() {
                            target.setBackground(
                                    drawable
                            );
                        }
                    }
            );

        } catch (Exception e) {

            e.printStackTrace();

            OnError(
                    "SetGradientBackground: " +
                            e.getMessage()
            );
        }
    }

    // ============================================================
    // BLUR
    // ============================================================

    @SimpleFunction(
            description = "Applique un effet de flou."
    )
    public void SetBlurEffect(
            final AndroidViewComponent component,
            final float radius) {

        if (component == null) {
            return;
        }

        runOnUi(
                new Runnable() {

                    @Override
                    public void run() {

                        try {

                            View view =
                                    component.getView();

                            if (view == null) {
                                return;
                            }

                            if (Build.VERSION.SDK_INT >=
                                    Build.VERSION_CODES.S) {

                                float blur =
                                        Math.max(
                                                1f,
                                                Math.min(
                                                        radius,
                                                        25f
                                                )
                                        );

                                view.setRenderEffect(
                                        RenderEffect
                                                .createBlurEffect(
                                                        blur,
                                                        blur,
                                                        Shader.TileMode.CLAMP
                                                )
                                );

                            } else {

                                view.setBackgroundColor(
                                        Color.argb(
                                                150,
                                                255,
                                                255,
                                                255
                                        )
                                );
                            }

                        } catch (Exception e) {

                            e.printStackTrace();

                            OnError(
                                    "SetBlurEffect: " +
                                            e.getMessage()
                            );
                        }
                    }
                }
        );
    }

    // ============================================================
    // DIALOG
    // ============================================================

    @SimpleFunction(
            description = "Affiche une boîte de dialogue."
    )
    public void ShowAlphaDialog(
            final String title,
            final String message,
            final String buttonText) {

        runOnUi(
                new Runnable() {

                    @Override
                    public void run() {

                        try {

                            if (currentAlphaDialog != null &&
                                    currentAlphaDialog.isShowing()) {

                                currentAlphaDialog.dismiss();
                            }

                            AlertDialog.Builder builder =
                                    new AlertDialog.Builder(
                                            activity
                                    );

                            builder.setTitle(
                                    title
                            );

                            builder.setMessage(
                                    message
                            );

                            builder.setPositiveButton(
                                    buttonText,
                                    new android.content.DialogInterface
                                            .OnClickListener() {

                                        @Override
                                        public void onClick(
                                                android.content.DialogInterface dialog,
                                                int which) {

                                            dialog.dismiss();

                                            AlphaDialogButtonClicked();
                                        }
                                    }
                            );

                            currentAlphaDialog =
                                    builder.create();

                            currentAlphaDialog.show();

                        } catch (Exception e) {

                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    @SimpleFunction(
            description = "Ferme la boîte de dialogue."
    )
    public void DismissAlphaDialog() {

        runOnUi(
                new Runnable() {

                    @Override
                    public void run() {

                        try {

                            if (currentAlphaDialog != null &&
                                    currentAlphaDialog.isShowing()) {

                                currentAlphaDialog.dismiss();
                            }

                            currentAlphaDialog =
                                    null;

                        } catch (Exception e) {

                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    // ============================================================
    // SON
    // ============================================================

    @SimpleFunction(
            description = "Joue un son personnalisé."
    )
    public void PlayCustomSound(
            final String soundPath) {

        if (soundPath == null ||
                soundPath.trim().isEmpty()) {

            OnError(
                    "PlayCustomSound: chemin vide."
            );

            return;
        }

        AsynchUtil.runAsynchronously(
                new Runnable() {

                    @Override
                    public void run() {

                        MediaPlayer player =
                                null;

                        try {

                            player =
                                    new MediaPlayer();

                            if (Build.VERSION.SDK_INT >=
                                    Build.VERSION_CODES.LOLLIPOP) {

                                player.setAudioAttributes(
                                        new AudioAttributes.Builder()
                                                .setContentType(
                                                        AudioAttributes
                                                                .CONTENT_TYPE_SONIFICATION
                                                )
                                                .setUsage(
                                                        AudioAttributes
                                                                .USAGE_ASSISTANCE_SONIFICATION
                                                )
                                                .build()
                                );
                            }

                            if (soundPath.startsWith(
                                    "http://"
                            ) ||
                                    soundPath.startsWith(
                                            "https://"
                                    )) {

                                player.setDataSource(
                                        soundPath
                                );

                            } else {

                                try {

                                    android.content.res
                                            .AssetFileDescriptor afd =
                                            context.getAssets()
                                                    .openFd(
                                                            soundPath
                                                    );

                                    player.setDataSource(
                                            afd.getFileDescriptor(),
                                            afd.getStartOffset(),
                                            afd.getLength()
                                    );

                                    afd.close();

                                } catch (Exception assetError) {

                                    player.setDataSource(
                                            soundPath
                                    );
                                }
                            }

                            final MediaPlayer finalPlayer =
                                    player;

                            player.setOnCompletionListener(
                                    new MediaPlayer
                                            .OnCompletionListener() {

                                        @Override
                                        public void onCompletion(
                                                MediaPlayer mp) {

                                            try {
                                                mp.release();
                                            } catch (Exception ignored) {
                                            }
                                        }
                                    }
                            );

                            player.setOnErrorListener(
                                    new MediaPlayer
                                            .OnErrorListener() {

                                        @Override
                                        public boolean onError(
                                                MediaPlayer mp,
                                                int what,
                                                int extra) {

                                            try {
                                                mp.release();
                                            } catch (Exception ignored) {
                                            }

                                            return true;
                                        }
                                    }
                            );

                            player.prepare();

                            player.start();

                        } catch (Exception e) {

                            if (player != null) {

                                try {
                                    player.release();
                                } catch (Exception ignored) {
                                }
                            }

                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    // ============================================================
    // COMPRESSION IMAGE
    // ============================================================

    @SimpleFunction(
            description = "Compresse une image."
    )
    public void CompressImage(
            final String imagePath,
            final int quality) {

        if (imagePath == null ||
                imagePath.trim().isEmpty()) {

            ImageCompressed("");

            return;
        }

        AsynchUtil.runAsynchronously(
                new Runnable() {

                    @Override
                    public void run() {

                        String compressedPath =
                                "";

                        Bitmap bitmap =
                                null;

                        InputStream input =
                                null;

                        FileOutputStream output =
                                null;

                        try {

                            if (imagePath.startsWith(
                                    "content://"
                            )) {

                                input =
                                        context
                                                .getContentResolver()
                                                .openInputStream(
                                                        Uri.parse(
                                                                imagePath
                                                        )
                                                );

                                if (input != null) {

                                    bitmap =
                                            BitmapFactory
                                                    .decodeStream(
                                                            input
                                                    );
                                }

                            } else if (
                                    imagePath.startsWith(
                                            "file://"
                                    )) {

                                input =
                                        context
                                                .getContentResolver()
                                                .openInputStream(
                                                        Uri.parse(
                                                                imagePath
                                                        )
                                                );

                                if (input != null) {

                                    bitmap =
                                            BitmapFactory
                                                    .decodeStream(
                                                            input
                                                    );
                                }

                            } else {

                                try {

                                    input =
                                            context
                                                    .getAssets()
                                                    .open(
                                                            imagePath
                                                    );

                                    bitmap =
                                            BitmapFactory
                                                    .decodeStream(
                                                            input
                                                    );

                                } catch (Exception e1) {

                                    try {

                                        bitmap =
                                                MediaUtil
                                                        .getBitmapDrawable(
                                                                form,
                                                                imagePath
                                                        )
                                                        .getBitmap();

                                    } catch (Exception e2) {

                                        File file =
                                                new File(
                                                        imagePath
                                                );

                                        if (file.exists()) {

                                            input =
                                                    new FileInputStream(
                                                            file
                                                    );

                                            bitmap =
                                                    BitmapFactory
                                                            .decodeStream(
                                                                    input
                                                            );
                                        }
                                    }
                                }
                            }

                            if (bitmap != null) {

                                File outputDir =
                                        context.getCacheDir();

                                File outputFile =
                                        File.createTempFile(
                                                "compressed_",
                                                ".jpg",
                                                outputDir
                                        );

                                output =
                                        new FileOutputStream(
                                                outputFile
                                        );

                                bitmap.compress(
                                        Bitmap.CompressFormat.JPEG,
                                        Math.max(
                                                0,
                                                Math.min(
                                                        100,
                                                        quality
                                                )
                                        ),
                                        output
                                );

                                output.flush();

                                compressedPath =
                                        outputFile
                                                .getAbsolutePath();
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

                            if (output != null) {

                                try {
                                    output.close();
                                } catch (Exception ignored) {
                                }
                            }

                            if (bitmap != null &&
                                    !bitmap.isRecycled()) {

                                bitmap.recycle();
                            }
                        }

                        final String result =
                                compressedPath;

                        runOnUi(
                                new Runnable() {

                                    @Override
                                    public void run() {
                                        ImageCompressed(
                                                result
                                        );
                                    }
                                }
                        );
                    }
                }
        );
    }

    // ============================================================
    // SERVEUR
    // ============================================================

    @SimpleFunction(
            description = "Effectue une requête HTTP ou HTTPS."
    )
    public void CallServerRequest(
            final String requestUrl,
            final String method) {

        if (requestUrl == null ||
                requestUrl.trim().isEmpty()) {

            OnServerResponse(
                    400,
                    "URL vide"
            );

            return;
        }

        AsynchUtil.runAsynchronously(
                new Runnable() {

                    @Override
                    public void run() {

                        HttpURLConnection connection =
                                null;

                        InputStream input =
                                null;

                        String result =
                                "";

                        int responseCode =
                                500;

                        try {

                            URL url =
                                    new URL(
                                            requestUrl
                                    );

                            connection =
                                    (HttpURLConnection)
                                            url.openConnection();

                            String requestMethod =
                                    method == null ||
                                            method.trim().isEmpty()
                                            ? "GET"
                                            : method
                                                    .trim()
                                                    .toUpperCase();

                            if (!requestMethod.equals("GET") &&
                                    !requestMethod.equals("POST") &&
                                    !requestMethod.equals("PUT") &&
                                    !requestMethod.equals("DELETE") &&
                                    !requestMethod.equals("PATCH")) {

                                requestMethod =
                                        "GET";
                            }

                            connection.setRequestMethod(
                                    requestMethod
                            );

                            connection.setConnectTimeout(
                                    15000
                            );

                            connection.setReadTimeout(
                                    15000
                            );

                            connection.setUseCaches(
                                    false
                            );

                            connection.setDoInput(
                                    true
                            );

                            responseCode =
                                    connection.getResponseCode();

                            if (responseCode >= 200 &&
                                    responseCode < 400) {

                                input =
                                        connection
                                                .getInputStream();

                            } else {

                                input =
                                        connection
                                                .getErrorStream();
                            }

                            result =
                                    readStream(
                                            input
                                    );

                        } catch (Exception e) {

                            result =
                                    "Error: " +
                                            e.getMessage();

                        } finally {

                            if (input != null) {

                                try {
                                    input.close();
                                } catch (Exception ignored) {
                                }
                            }

                            if (connection != null) {
                                connection.disconnect();
                            }
                        }

                        final String finalResult =
                                result;

                        final int finalCode =
                                responseCode;

                        runOnUi(
                                new Runnable() {

                                    @Override
                                    public void run() {

                                        ServerResponseReceived(
                                                finalResult
                                        );

                                        OnServerResponse(
                                                finalCode,
                                                finalResult
                                        );
                                    }
                                }
                        );
                    }
                }
        );
    }

    private String readStream(
            InputStream input) {

        if (input == null) {
            return "";
        }

        try {

            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

            byte[] buffer =
                    new byte[4096];

            int count;

            while (
                    (count =
                            input.read(buffer)) != -1
            ) {

                output.write(
                        buffer,
                        0,
                        count
                );
            }

            return output.toString(
                    "UTF-8"
            );

        } catch (Exception e) {

            return "";
        }
    }

    // ============================================================
    // GALERIE
    // ============================================================

    @SimpleFunction(
            description = "Ouvre le sélecteur d'images."
    )
    public void OpenPhotoPicker() {

        String permission;

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU) {

            permission =
                    "android.permission.READ_MEDIA_IMAGES";

        } else {

            permission =
                    "android.permission.READ_EXTERNAL_STORAGE";
        }

        form.askPermission(
                permission,
                new Form.PermissionResultHandler() {

                    @Override
                    public void HandlePermissionResponse(
                            String permissionName,
                            boolean granted) {

                        if (!granted) {

                            OnError(
                                    "Permission galerie refusée."
                            );

                            return;
                        }

                        openImagePicker();
                    }
                }
        );
    }

    private void openImagePicker() {

        runOnUi(
                new Runnable() {

                    @Override
                    public void run() {

                        try {

                            Intent intent =
                                    new Intent(
                                            Intent.ACTION_PICK
                                    );

                            intent.setType(
                                    "image/*"
                            );

                            form.startActivityForResult(
                                    intent,
                                    requestCode
                            );

                        } catch (Exception e) {

                            OnError(
                                    "OpenPhotoPicker: " +
                                            e.getMessage()
                            );
                        }
                    }
                }
        );
    }

    @Override
    public void resultReturned(
            int receivedRequestCode,
            int resultCode,
            Intent data) {

        if (receivedRequestCode !=
                requestCode) {
            return;
        }

        if (resultCode ==
                Activity.RESULT_OK &&
                data != null) {

            Uri uri =
                    data.getData();

            if (uri != null) {

                OnPhotoPicked(
                        uri.toString()
                );

            } else {

                OnError(
                        "URI image nulle."
                );
            }

        } else {

            OnError(
                    "Sélection annulée."
            );
        }
    }

    // ============================================================
    // ÉVÉNEMENTS
    // ============================================================

    @SimpleEvent(
            description = "Sélection d'un onglet."
    )
    public void OnSelected(
            String id) {

        EventDispatcher.dispatchEvent(
                this,
                "OnSelected",
                id
        );
    }

    @SimpleEvent(
            description = "Sélection d'un produit."
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
            description = "Clic sur un avatar."
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
            description = "Clic sur l'avatar d'un utilisateur."
    )
    public void OnUserAvatarClick(
            String userUid,
            String userRole) {

        EventDispatcher.dispatchEvent(
                this,
                "OnUserAvatarClick",
                userUid,
                userRole
        );
    }

    @SimpleEvent(
            description = "Sélection d'une catégorie."
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
            description = "Image sélectionnée."
    )
    public void OnPhotoPicked(
            String imageUri) {

        EventDispatcher.dispatchEvent(
                this,
                "OnPhotoPicked",
                imageUri
        );
    }

    @SimpleEvent(
            description = "Image compressée."
    )
    public void ImageCompressed(
            String path) {

        EventDispatcher.dispatchEvent(
                this,
                "ImageCompressed",
                path
        );
    }

    @SimpleEvent(
            description = "Réponse serveur."
    )
    public void ServerResponseReceived(
            String response) {

        EventDispatcher.dispatchEvent(
                this,
                "ServerResponseReceived",
                response
        );
    }

    @SimpleEvent(
            description = "Réponse serveur avec code HTTP."
    )
    public void OnServerResponse(
            int responseCode,
            String responseContent) {

        EventDispatcher.dispatchEvent(
                this,
                "OnServerResponse",
                responseCode,
                responseContent
        );
    }

    @SimpleEvent(
            description = "Bouton du dialogue sélectionné."
    )
    public void AlphaDialogButtonClicked() {

        EventDispatcher.dispatchEvent(
                this,
                "AlphaDialogButtonClicked"
        );
    }

    @SimpleEvent(
            description = "Erreur de l'extension."
    )
    public void OnError(
            String message) {

        EventDispatcher.dispatchEvent(
                this,
                "OnError",
                message
        );
    }

    // ============================================================
    // COMPATIBILITÉ ICÔNES
    // ============================================================

    private static class DrawableCompatHelper {

        static void setIcon(
                ImageView image,
                Form form,
                String path)
                throws Exception {

            if (image == null ||
                    form == null ||
                    path == null ||
                    path.trim().isEmpty()) {

                return;
            }

            image.setImageDrawable(
                    MediaUtil.getBitmapDrawable(
                            form,
                            path
                    )
            );
        }
    }

    // ============================================================
    // FIN DE ManaplaceUtils
    // ============================================================

                    }
