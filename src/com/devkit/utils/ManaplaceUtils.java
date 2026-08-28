package com.devkit.utils;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
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
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
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
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

@DesignerComponent(
        version = 16,
        description = "Extension ManaplaceUtils - corrections: clavier flottant, auto-grow, galerie/permissions, barre de navigation, réutilisation multiple sans conflit parent/dialogue + détails produits + badges génériques.",
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
    private View activeDialogContentView;

    private Typeface customTypeface = Typeface.DEFAULT;
    private int radioButtonColor = Color.parseColor("#C01A1A1B");

    // =========================================================================
    // WEBSOCKET (TEMPS RÉEL)
    // =========================================================================

    private Socket wsSocket;
    private OutputStream wsOutput;
    private volatile boolean wsRunning = false;

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
    private View navBarView = null;

    // =========================================================================
    // BADGES GÉNÉRIQUES
    // =========================================================================
    //
    // Le badge est placé directement dans la racine de l'activité.
    // Le composant d'origine n'est donc PAS modifié et garde exactement
    // sa taille, son parent et son comportement.
    //
    // Cela permet d'utiliser les badges sur :
    // CardView
    // Button
    // Image
    // Arrangement
    // Label
    // etc.
    //
    // =========================================================================

    private final Map<View, TextView> badges = new HashMap<>();
    private final Map<View, Integer> badgeCounts = new HashMap<>();
    private final Map<View, String> badgeTexts = new HashMap<>();
    private final Map<View, Integer> badgeColors = new HashMap<>();
    private final Map<View, Integer> badgeTextColors = new HashMap<>();
    private final Map<View, Boolean> badgeVisibility = new HashMap<>();

    private ViewTreeObserver.OnGlobalLayoutListener badgeLayoutListener;
    private boolean badgeListenerInstalled = false;

    private int defaultBadgeColor = Color.parseColor("#E53935");
    private int defaultBadgeTextColor = Color.WHITE;

    // =========================================================================
    // DÉTAILS PRODUIT
    // =========================================================================

    private String productDetailsEndpoint = "";

    private String currentProductUid = "";
    private String currentProductDescription = "";
    private int currentProductStock = 0;

    private String currentProductImage2 = "";
    private String currentProductImage3 = "";
    private String currentProductImage4 = "";
    private String currentProductImage5 = "";

    private boolean currentProductDeliveryIncluded = false;

    private boolean productDetailsLoading = false;

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

    // =========================================================================
    // BARRE DE NAVIGATION
    // =========================================================================

    @SimpleFunction(
            description = "Ajoute une icône à la barre de navigation. À appeler une fois par icône, avant NavBarInitialize."
    )
    public void NavBarAdd(String id, String icon) {

        if (idsEnAttente.contains(id)) {
            NavBarError("Id déjà utilisé: " + id);
            return;
        }

        idsEnAttente.add(id);
        iconesEnAttente.add(icon);
    }

    @SimpleFunction(
            description = "Construit et affiche la barre flottante avec toutes les icônes ajoutées via NavBarAdd."
    )
    public void NavBarInitialize(
            final int margeBas,
            final double largeurPourcent,
            final double hauteurPourcent) {

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                try {

                    FrameLayout root =
                            (FrameLayout) activity.findViewById(android.R.id.content);

                    if (root == null) {
                        NavBarError("Écran racine introuvable");
                        return;
                    }

                    if (navBarView != null &&
                            root.indexOfChild(navBarView) != -1) {

                        root.removeView(navBarView);
                    }

                    vuesIcones.clear();
                    vuesCercles.clear();
                    idsFinaux.clear();
                    idSelectionne = null;

                    if (idsEnAttente.isEmpty()) {

                        NavBarError(
                                "Aucune icône ajoutée — appelle NavBarAdd avant NavBarInitialize"
                        );

                        return;
                    }

                    DisplayMetrics metrics =
                            activity.getResources().getDisplayMetrics();

                    int largeurFinale =
                            (largeurPourcent > 0)
                                    ? (int) (
                                    metrics.widthPixels *
                                            (largeurPourcent / 100.0)
                            )
                                    : ViewGroup.LayoutParams.WRAP_CONTENT;

                    int hauteurFinale =
                            (hauteurPourcent > 0)
                                    ? (int) (
                                    metrics.heightPixels *
                                            (hauteurPourcent / 100.0)
                            )
                                    : (int) dpToPx(64);

                    LinearLayout bar = new LinearLayout(activity);

                    bar.setOrientation(LinearLayout.HORIZONTAL);
                    bar.setGravity(Gravity.CENTER);
                    bar.setWeightSum(idsEnAttente.size());
                    bar.setElevation(dpToPx(8));

                    GradientDrawable fond = new GradientDrawable();

                    fond.setColor(Color.WHITE);
                    fond.setCornerRadius(dpToPx(30));

                    bar.setBackground(fond);

                    for (int i = 0;
                         i < idsEnAttente.size();
                         i++) {

                        final String tabId =
                                idsEnAttente.get(i);

                        String iconFile =
                                iconesEnAttente.get(i);

                        FrameLayout conteneur =
                                new FrameLayout(activity);

                        conteneur.setLayoutParams(
                                new LinearLayout.LayoutParams(
                                        0,
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        1f
                                )
                        );

                        View cercle =
                                new View(activity);

                        GradientDrawable fondCercle =
                                new GradientDrawable();

                        fondCercle.setShape(
                                GradientDrawable.OVAL
                        );

                        fondCercle.setColor(
                                Color.argb(
                                        30,
                                        0,
                                        0,
                                        0
                                )
                        );

                        cercle.setBackground(fondCercle);
                        cercle.setAlpha(0f);

                        conteneur.addView(
                                cercle,
                                new FrameLayout.LayoutParams(
                                        (int) dpToPx(46),
                                        (int) dpToPx(46),
                                        Gravity.CENTER
                                )
                        );

                        ImageView img =
                                new ImageView(activity);

                        img.setAdjustViewBounds(true);

                        try {

                            Drawable d =
                                    MediaUtil.getBitmapDrawable(
                                            monForm,
                                            iconFile
                                    );

                            img.setImageDrawable(d);

                            img.setColorFilter(
                                    new PorterDuffColorFilter(
                                            Color.rgb(
                                                    150,
                                                    150,
                                                    150
                                            ),
                                            PorterDuff.Mode.SRC_IN
                                    )
                            );

                        } catch (IOException e) {

                            NavBarError(
                                    "Icône introuvable: " +
                                            iconFile
                            );
                        }

                        int taillePx =
                                (int) dpToPx(
                                        tailleIconeDp
                                );

                        conteneur.addView(
                                img,
                                new FrameLayout.LayoutParams(
                                        taillePx,
                                        taillePx,
                                        Gravity.CENTER
                                )
                        );

                        final View cercleFinal = cercle;
                        final ImageView imgFinal = img;

                        conteneur.setOnClickListener(
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {

                                        SelectionnerOnglet(
                                                tabId,
                                                cercleFinal,
                                                imgFinal
                                        );
                                    }
                                }
                        );

                        vuesIcones.add(img);
                        vuesCercles.add(cercle);
                        idsFinaux.add(tabId);

                        bar.addView(conteneur);
                    }

                    FrameLayout.LayoutParams params =
                            new FrameLayout.LayoutParams(
                                    largeurFinale,
                                    hauteurFinale
                            );

                    params.gravity =
                            Gravity.BOTTOM |
                                    Gravity.CENTER_HORIZONTAL;

                    params.setMargins(
                            0,
                            0,
                            0,
                            (int) dpToPx(margeBas)
                    );

                    root.addView(bar, params);

                    navBarView = bar;

                    dejaInitialise = true;

                    // Les badges existants sont repositionnés.
                    updateAllBadges();

                } catch (Exception e) {

                    NavBarError(
                            "NavBarInitialize: " +
                                    e.getMessage()
                    );
                }
            }
        });
    }

    @SimpleFunction(
            description = "Ajuste la taille de toutes les icônes de la barre en dp, avec une transition animée."
    )
    public void NavBarSetIconSize(final int tailleDp) {

        final int ancienneTailleDp =
                tailleIconeDp;

        tailleIconeDp = tailleDp;

        if (vuesIcones.isEmpty())
            return;

        try {

            final float ancienPx =
                    dpToPx(
                            ancienneTailleDp
                    );

            final float nouveauPx =
                    dpToPx(tailleDp);

            ValueAnimator anim =
                    ValueAnimator.ofFloat(
                            ancienPx,
                            nouveauPx
                    );

            anim.setDuration(220);

            anim.setInterpolator(
                    new DecelerateInterpolator()
            );

            anim.addUpdateListener(
                    new ValueAnimator.AnimatorUpdateListener() {

                        @Override
                        public void onAnimationUpdate(
                                ValueAnimator animation) {

                            int taillePx =
                                    (int)
                                            (float)
                                                    animation
                                                            .getAnimatedValue();

                            for (ImageView iv :
                                    vuesIcones) {

                                ViewGroup.LayoutParams p =
                                        iv.getLayoutParams();

                                p.width = taillePx;
                                p.height = taillePx;

                                iv.setLayoutParams(p);
                            }

                            updateAllBadges();
                        }
                    }
            );

            anim.start();

        } catch (Exception e) {

            NavBarError(
                    "Erreur NavBarSetIconSize: " +
                            e.getMessage()
            );
        }
    }

    @SimpleFunction(
            description = "Sélectionne un onglet de la barre par code, sans clic."
    )
    public void NavBarSelect(String id) {

        int index =
                idsFinaux.indexOf(id);

        if (index < 0) {

            NavBarError(
                    "Id introuvable pour NavBarSelect: " +
                            id
            );

            return;
        }

        SelectionnerOnglet(
                id,
                vuesCercles.get(index),
                vuesIcones.get(index)
        );
    }

    @SimpleFunction(
            description = "Affiche ou masque la barre de navigation."
    )
    public void NavBarSetVisible(
            final boolean visible) {

        if (navBarView == null) {

            NavBarError(
                    "NavBarSetVisible: la barre n'est pas encore initialisée."
            );

            return;
        }

        activity.runOnUiThread(
                new Runnable() {

                    @Override
                    public void run() {

                        navBarView.setVisibility(
                                visible
                                        ? View.VISIBLE
                                        : View.GONE
                        );

                        updateAllBadges();
                    }
                }
        );
    }

    // =========================================================================
    // BADGE SUR LA NAVBAR
    // =========================================================================

    @SimpleFunction(
            description = "Ajoute ou modifie un badge numérique sur une icône de la barre de navigation."
    )
    public void NavBarSetBadge(
            final String id,
            final int count) {

        activity.runOnUiThread(
                new Runnable() {

                    @Override
                    public void run() {

                        int index =
                                idsFinaux.indexOf(id);

                        if (index < 0) {

                            NavBarError(
                                    "Badge: id NavBar introuvable: " +
                                            id
                            );

                            return;
                        }

                        View target =
                                vuesIcones.get(index);

                        setBadgeInternal(
                                target,
                                count,
                                null
                        );
                    }
                }
        );
    }

    @SimpleFunction(
            description = "Affiche un texte personnalisé comme badge sur une icône de la barre."
    )
    public void NavBarSetBadgeText(
            final String id,
            final String text) {

        activity.runOnUiThread(
                new Runnable() {

                    @Override
                    public void run() {

                        int index =
                                idsFinaux.indexOf(id);

                        if (index < 0) {

                            NavBarError(
                                    "Badge: id NavBar introuvable: " +
                                            id
                            );

                            return;
                        }

                        setBadgeInternal(
                                vuesIcones.get(index),
                                0,
                                text
                        );
                    }
                }
        );
    }

    @SimpleFunction(
            description = "Supprime le badge d'une icône de la barre de navigation."
    )
    public void NavBarClearBadge(final String id) {

        final int index = idsFinaux.indexOf(id);

        if (index < 0) {
            NavBarError("Badge: id NavBar introuvable: " + id);
            return;
        }

        final View target = vuesIcones.get(index);

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                removeBadgeInternal(target);
            }
        });
    }

    private void SelectionnerOnglet(
            String id,
            View cercle,
            ImageView img) {

        try {

            if (id.equals(idSelectionne))
                return;

            if (idSelectionne != null) {

                int ancienIndex =
                        idsFinaux.indexOf(
                                idSelectionne
                        );

                if (ancienIndex >= 0) {

                    animerOnglet(
                            vuesCercles.get(ancienIndex),
                            vuesIcones.get(ancienIndex),
                            false
                    );
                }
            }

            animerOnglet(
                    cercle,
                    img,
                    true
            );

            idSelectionne = id;

            OnSelected(id);

        } catch (Exception e) {

            NavBarError(
                    "Erreur de sélection: " +
                            e.getMessage()
            );
        }
    }

    private void animerOnglet(
            final View cercle,
            final ImageView img,
            boolean selectionne) {

        float alphaCible =
                selectionne
                        ? 1f
                        : 0f;

        ValueAnimator anim =
                ValueAnimator.ofFloat(
                        cercle.getAlpha(),
                        alphaCible
                );

        anim.setDuration(220);

        anim.setInterpolator(
                new DecelerateInterpolator()
        );

        anim.addUpdateListener(
                new ValueAnimator.AnimatorUpdateListener() {

                    @Override
                    public void onAnimationUpdate(
                            ValueAnimator animation) {

                        float val =
                                (float)
                                        animation
                                                .getAnimatedValue();

                        cercle.setAlpha(val);

                        int couleur =
                                melangerCouleurs(
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
                                        val
                                );

                        img.setColorFilter(
                                new PorterDuffColorFilter(
                                        couleur,
                                        PorterDuff.Mode.SRC_IN
                                )
                        );
                    }
                }
        );

        anim.start();
    }

    private int melangerCouleurs(
            int c1,
            int c2,
            float ratio) {

        int r =
                (int)
                        (
                                Color.red(c1) +
                                        ratio *
                                                (
                                                        Color.red(c2) -
                                                                Color.red(c1)
                                                )
                        );

        int g =
                (int)
                        (
                                Color.green(c1) +
                                        ratio *
                                                (
                                                        Color.green(c2) -
                                                                Color.green(c1)
                                                )
                        );

        int b =
                (int)
                        (
                                Color.blue(c1) +
                                        ratio *
                                                (
                                                        Color.blue(c2) -
                                                                Color.blue(c1)
                                                )
                        );

        return Color.rgb(r, g, b);
    }

    // =========================================================================
    // SYSTÈME DE BADGES GÉNÉRIQUE
    // =========================================================================

    @SimpleFunction(
            description = "Ajoute un badge numérique à n'importe quel composant Android."
    )
    public void AddBadge(
            final AndroidViewComponent component,
            final int count) {

        if (component == null ||
                component.getView() == null) {

            OnError(
                    "AddBadge: composant invalide."
            );

            return;
        }

        activity.runOnUiThread(
                new Runnable() {

                    @Override
                    public void run() {

                        setBadgeInternal(
                                component.getView(),
                                count,
                                null
                        );
                    }
                }
        );
    }

    @SimpleFunction(
            description = "Définit le nombre affiché par un badge existant."
    )
    public void SetBadgeCount(
            final AndroidViewComponent component,
            final int count) {

        if (component == null ||
                component.getView() == null) {

            OnError(
                    "SetBadgeCount: composant invalide."
            );

            return;
        }

        activity.runOnUiThread(
                new Runnable() {

                    @Override
                    public void run() {

                        setBadgeInternal(
                                component.getView(),
                                count,
                                null
                        );
                    }
                }
        );
    }

    @SimpleFunction(
            description = "Ajoute ou modifie un badge avec un texte personnalisé."
    )
    public void SetBadgeText(
            final AndroidViewComponent component,
            final String text) {

        if (component == null ||
                component.getView() == null) {

            OnError(
                    "SetBadgeText: composant invalide."
            );

            return;
        }

        activity.runOnUiThread(
                new Runnable() {

                    @Override
                    public void run() {

                        setBadgeInternal(
                                component.getView(),
                                0,
                                text
                        );
                    }
                }
        );
    }

    @SimpleFunction(
            description = "Change la couleur du badge."
    )
    public void SetBadgeColor(
            final AndroidViewComponent component,
            final int color) {

        if (component == null ||
                component.getView() == null) {

            return;
        }

        activity.runOnUiThread(
                new Runnable() {

                    @Override
                    public void run() {

                        View target =
                                component.getView();

                        badgeColors.put(
                                target,
                                color
                        );

                        TextView badge =
                                badges.get(target);

                        if (badge != null) {

                            applyBadgeBackground(
                                    badge,
                                    color
                            );
                        }
                    }
                }
        );
    }

    @SimpleFunction(
            description = "Change la couleur du texte du badge."
    )
    public void SetBadgeTextColor(
            final AndroidViewComponent component,
            final int color) {

        if (component == null ||
                component.getView() == null) {

            return;
        }

        activity.runOnUiThread(
                new Runnable() {

                    @Override
                    public void run() {

                        View target =
                                component.getView();

                        badgeTextColors.put(
                                target,
                                color
                        );

                        TextView badge =
                                badges.get(target);

                        if (badge != null) {
                            badge.setTextColor(color);
                        }
                    }
                }
        );
    }

    @SimpleFunction(
            description = "Affiche ou masque le badge d'un composant."
    )
    public void SetBadgeVisible(
            final AndroidViewComponent component,
            final boolean visible) {

        if (component == null ||
                component.getView() == null) {

            return;
        }

        activity.runOnUiThread(
                new Runnable() {

                    @Override
                    public void run() {

                        View target =
                                component.getView();

                        badgeVisibility.put(
                                target,
                                visible
                        );

                        TextView badge =
                                badges.get(target);

                        if (badge != null) {

                            badge.setVisibility(
                                    visible
                                            ? View.VISIBLE
                                            : View.GONE
                            );

                            updateBadgePosition(
                                    target,
                                    badge
                            );
                        }
                    }
                }
        );
    }

    @SimpleFunction(
            description = "Supprime complètement le badge d'un composant."
    )
    public void RemoveBadge(
            final AndroidViewComponent component) {

        if (component == null ||
                component.getView() == null) {

            return;
        }

        activity.runOnUiThread(
                new Runnable() {

                    @Override
                    public void run() {

                        removeBadgeInternal(
                                component.getView()
                        );
                    }
                }
        );
    }

    @SimpleFunction(
            description = "Définit la couleur par défaut utilisée pour les nouveaux badges."
    )
    public void SetDefaultBadgeColor(int color) {

        defaultBadgeColor = color;
    }

    @SimpleFunction(
            description = "Définit la couleur de texte par défaut utilisée pour les nouveaux badges."
    )
    public void SetDefaultBadgeTextColor(int color) {

        defaultBadgeTextColor = color;
    }

    private void setBadgeInternal(
            final View target,
            final int count,
            final String customText) {

        if (target == null)
            return;

        FrameLayout root =
                (FrameLayout)
                        activity.findViewById(
                                android.R.id.content
                        );

        if (root == null) {

            OnError(
                    "Badge: racine de l'activité introuvable."
            );

            return;
        }

        TextView badge =
                badges.get(target);

        if (badge == null) {

            badge = new TextView(activity);

            badge.setGravity(
                    Gravity.CENTER
            );

            badge.setIncludeFontPadding(false);

            badge.setTypeface(
                    Typeface.DEFAULT,
                    Typeface.BOLD
            );

            badge.setTextSize(
                    TypedValue.COMPLEX_UNIT_SP,
                    10
            );

            badge.setTextColor(
                    defaultBadgeTextColor
            );

            badge.setMinWidth(
                    (int) dpToPx(18)
            );

            badge.setMinHeight(
                    (int) dpToPx(18)
            );

            badge.setPadding(
                    (int) dpToPx(4),
                    0,
                    (int) dpToPx(4),
                    0
            );

            applyBadgeBackground(
                    badge,
                    defaultBadgeColor
            );

            FrameLayout.LayoutParams params =
                    new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            (int) dpToPx(20)
                    );

            params.gravity =
                    Gravity.TOP |
                            Gravity.LEFT;

            root.addView(
                    badge,
                    params
            );

            badges.put(
                    target,
                    badge
            );

            badgeColors.put(
                    target,
                    defaultBadgeColor
            );

            badgeTextColors.put(
                    target,
                    defaultBadgeTextColor
            );

            badgeVisibility.put(
                    target,
                    true
            );

            installBadgeLayoutListener();
        }

        if (customText != null) {

            String text =
                    customText.trim();

            badgeTexts.put(
                    target,
                    text
            );

            badge.setText(text);

        } else {

            badgeCounts.put(
                    target,
                    count
            );

            String displayText;

            if (count <= 0) {

                displayText = "";

            } else if (count > 99) {

                displayText = "99+";

            } else {

                displayText =
                        String.valueOf(count);
            }

            badgeTexts.put(
                    target,
                    displayText
            );

            badge.setText(displayText);
        }

        int textColor =
                badgeTextColors.containsKey(target)
                        ? badgeTextColors.get(target)
                        : defaultBadgeTextColor;

        badge.setTextColor(textColor);

        int badgeColor =
                badgeColors.containsKey(target)
                        ? badgeColors.get(target)
                        : defaultBadgeColor;

        applyBadgeBackground(
                badge,
                badgeColor
        );

        boolean visible =
                badgeVisibility.containsKey(target)
                        ? badgeVisibility.get(target)
                        : true;

        if (customText != null) {
            visible =
                    !customText.trim().isEmpty();
        } else {
            visible =
                    count > 0;
        }

        badgeVisibility.put(
                target,
                visible
        );

        badge.setVisibility(
                visible
                        ? View.VISIBLE
                        : View.GONE
        );

        updateBadgeSize(
                target,
                badge
        );

        updateBadgePosition(
                target,
                badge
        );
    }

    private void applyBadgeBackground(
            TextView badge,
            int color) {

        GradientDrawable background =
                new GradientDrawable();

        background.setShape(
                GradientDrawable.OVAL
        );

        background.setColor(color);

        badge.setBackground(
                background
        );
    }

    private void updateBadgeSize(
            View target,
            TextView badge) {

        String text =
                badge.getText() == null
                        ? ""
                        : badge.getText().toString();

        int minWidth;

        if (text.length() >= 3) {

            minWidth =
                    (int) dpToPx(28);

        } else if (text.length() == 2) {

            minWidth =
                    (int) dpToPx(23);

        } else {

            minWidth =
                    (int) dpToPx(20);
        }

        int height =
                (int) dpToPx(20);

        ViewGroup.LayoutParams oldParams =
                badge.getLayoutParams();

        if (oldParams instanceof FrameLayout.LayoutParams) {

            FrameLayout.LayoutParams params =
                    (FrameLayout.LayoutParams)
                            oldParams;

            params.width =
                    minWidth;

            params.height =
                    height;

            badge.setLayoutParams(params);
        }
    }

    private void updateBadgePosition(
            View target,
            View badge) {

        if (target == null ||
                badge == null) {

            return;
        }

        if (target.getWindowToken() == null ||
                !target.isAttachedToWindow()) {

            badge.setVisibility(
                    View.GONE
            );

            return;
        }

        FrameLayout root =
                (FrameLayout)
                        activity.findViewById(
                                android.R.id.content
                        );

        if (root == null)
            return;

        int[] targetLocation =
                new int[2];

        int[] rootLocation =
                new int[2];

        target.getLocationOnScreen(
                targetLocation
        );

        root.getLocationOnScreen(
                rootLocation
        );

        int targetLeft =
                targetLocation[0] -
                        rootLocation[0];

        int targetTop =
                targetLocation[1] -
                        rootLocation[1];

        int targetWidth =
                target.getWidth();

        int badgeWidth =
                badge.getWidth();

        if (badgeWidth <= 0) {

            badgeWidth =
                    (int) dpToPx(20);
        }

        int badgeHeight =
                badge.getHeight();

        if (badgeHeight <= 0) {

            badgeHeight =
                    (int) dpToPx(20);
        }

        // Position en haut à droite.
        // Une partie du badge dépasse volontairement du composant.
        int left =
                targetLeft +
                        targetWidth -
                        badgeWidth / 2;

        int top =
                targetTop -
                        badgeHeight / 2;

        FrameLayout.LayoutParams params;

        if (badge.getLayoutParams()
                instanceof FrameLayout.LayoutParams) {

            params =
                    (FrameLayout.LayoutParams)
                            badge.getLayoutParams();

        } else {

            params =
                    new FrameLayout.LayoutParams(
                            badgeWidth,
                            badgeHeight
                    );
        }

        params.leftMargin =
                left;

        params.topMargin =
                top;

        params.gravity =
                Gravity.TOP |
                        Gravity.LEFT;

        badge.setLayoutParams(params);
    }

    private void updateAllBadges() {

        if (badges.isEmpty())
            return;

        activity.runOnUiThread(
                new Runnable() {

                    @Override
                    public void run() {

                        for (
                                Map.Entry<View, TextView> entry :
                                badges.entrySet()
                        ) {

                            View target =
                                    entry.getKey();

                            TextView badge =
                                    entry.getValue();

                            if (target != null &&
                                    badge != null) {

                                updateBadgePosition(
                                        target,
                                        badge
                                );
                            }
                        }
                    }
                }
        );
    }

    private void removeBadgeInternal(
            View target) {

        if (target == null)
            return;

        TextView badge =
                badges.remove(target);

        badgeCounts.remove(target);
        badgeTexts.remove(target);
        badgeColors.remove(target);
        badgeTextColors.remove(target);
        badgeVisibility.remove(target);

        if (badge != null) {

            ViewGroup parent =
                    (ViewGroup)
                            badge.getParent();

            if (parent != null) {
                parent.removeView(badge);
            }
        }

        if (badges.isEmpty()) {
            uninstallBadgeLayoutListener();
        }
    }

    private void installBadgeLayoutListener() {

        if (badgeListenerInstalled)
            return;

        final View rootView =
                activity.getWindow()
                        .getDecorView()
                        .getRootView();

        badgeLayoutListener =
                new ViewTreeObserver.OnGlobalLayoutListener() {

                    @Override
                    public void onGlobalLayout() {

                        updateAllBadges();
                    }
                };

        rootView.getViewTreeObserver()
                .addOnGlobalLayoutListener(
                        badgeLayoutListener
                );

        badgeListenerInstalled = true;
    }

    private void uninstallBadgeLayoutListener() {

        if (!badgeListenerInstalled)
            return;

        try {

            View rootView =
                    activity.getWindow()
                            .getDecorView()
                            .getRootView();

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.JELLY_BEAN) {

                rootView.getViewTreeObserver()
                        .removeOnGlobalLayoutListener(
                                badgeLayoutListener
                        );

            } else {

                rootView.getViewTreeObserver()
                        .removeGlobalOnLayoutListener(
                                badgeLayoutListener
                        );
            }

        } catch (Exception ignored) {
        }

        badgeListenerInstalled = false;
        badgeLayoutListener = null;
    }

    // =========================================================================
    // POLICE PERSONNALISÉE
    // =========================================================================

    @SimpleFunction(
            description = "Charge une police personnalisée .ttf ou .otf."
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

            e.printStackTrace();

            customTypeface =
                    Typeface.DEFAULT;
        }
    }

    @SimpleFunction(
            description = "Définit la couleur des boutons radio."
    )
    public void SetRadioButtonColor(int color) {

        radioButtonColor = color;
    }

    // =========================================================================
    // UTILITAIRES IMAGES
    // =========================================================================

    private ViewGroup getRealLayout(
            AndroidViewComponent component) {

        if (component == null)
            return null;

        View view =
                component.getView();

        if (view instanceof ViewGroup) {

            ViewGroup vg =
                    (ViewGroup) view;

            if (vg.getChildCount() > 0 &&
                    vg.getChildAt(0)
                            instanceof ViewGroup) {

                return (ViewGroup)
                        vg.getChildAt(0);
            }

            return vg;
        }

        return null;
    }

    private void runOnUi(
            Runnable runnable) {

        activity.runOnUiThread(
                runnable
        );
    }

    private void loadImageAsync(
            final ImageView imageView,
            final String imagePath) {

        if (imagePath == null ||
                imagePath.trim().isEmpty())
            return;

        AsynchUtil.runAsynchronously(
                new Runnable() {

                    @Override
                    public void run() {

                        Bitmap bmp = null;
                        InputStream input = null;
                        HttpURLConnection conn = null;

                        try {

                            if (imagePath.startsWith(
                                    "http://"
                            ) ||
                                    imagePath.startsWith(
                                            "https://"
                                    )) {

                                URL url =
                                        new URL(imagePath);

                                conn =
                                        (HttpURLConnection)
                                                url.openConnection();

                                conn.setConnectTimeout(
                                        15000
                                );

                                conn.setReadTimeout(
                                        15000
                                );

                                conn.setDoInput(true);

                                conn.connect();

                                input =
                                        conn.getInputStream();

                                bmp =
                                        BitmapFactory
                                                .decodeStream(
                                                        input
                                                );

                            } else if (
                                    imagePath.startsWith(
                                            "content://"
                                    )
                            ) {

                                input =
                                        context
                                                .getContentResolver()
                                                .openInputStream(
                                                        Uri.parse(
                                                                imagePath
                                                        )
                                                );

                                if (input != null) {

                                    bmp =
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

                                    bmp =
                                            BitmapFactory
                                                    .decodeStream(
                                                            input
                                                    );

                                } catch (Exception assetError) {

                                    try {

                                        bmp =
                                                MediaUtil
                                                        .getBitmapDrawable(
                                                                monForm,
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

                                            bmp =
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
                                } catch (Exception ignored) {}
                            }

                            if (conn != null) {
                                conn.disconnect();
                            }
                        }

                        final Bitmap finalBmp =
                                bmp;

                        if (finalBmp != null) {

                            activity.runOnUiThread(
                                    new Runnable() {

                                        @Override
                                        public void run() {

                                            if (imageView
                                                    .getWindowToken()
                                                    != null ||
                                                    imageView
                                                            .isAttachedToWindow()
                                            ) {

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
                }
        );
    }

    // =========================================================================
    // 1. MOTEUR DE CHAT DYNAMIQUE NATIF
    // =========================================================================

    @SimpleFunction(
            description = "Ajoute une bulle de chat avec un petit avatar rond."
    )
    public void AddChatBubble(
            final AndroidViewComponent chatContainer,
            final String messageText,
            final String timeText,
            final String avatarUrl,
            final String senderUid,
            final boolean isMe,
            final int bubbleColor,
            final int textColor) {

        runOnUi(
                new Runnable() {

                    @Override
                    public void run() {

                        try {

                            ViewGroup targetLayout =
                                    getRealLayout(
                                            chatContainer
                                    );

                            if (targetLayout == null)
                                return;

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
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                    );

                            rowParams.setMargins(
                                    (int) dpToPx(8),
                                    (int) dpToPx(4),
                                    (int) dpToPx(8),
                                    (int) dpToPx(4)
                            );

                            row.setLayoutParams(
                                    rowParams
                            );

                            int avatarSizePx =
                                    (int) dpToPx(32);

                            CardView avatarCard =
                                    new CardView(context);

                            LinearLayout.LayoutParams avatarParams =
                                    new LinearLayout.LayoutParams(
                                            avatarSizePx,
                                            avatarSizePx
                                    );

                            avatarParams.gravity =
                                    Gravity.CENTER_VERTICAL;

                            avatarParams.setMargins(
                                    (int) dpToPx(6),
                                    0,
                                    (int) dpToPx(6),
                                    0
                            );

                            avatarCard.setLayoutParams(
                                    avatarParams
                            );

                            avatarCard.setRadius(
                                    avatarSizePx / 2f
                            );

                            avatarCard.setCardElevation(0f);
                            avatarCard.setMaxCardElevation(0f);

                            avatarCard.setCardBackgroundColor(
                                    Color.parseColor(
                                            "#E0E0E0"
                                    )
                            );

                            ImageView avatarImg =
                                    new ImageView(context);

                            avatarImg.setLayoutParams(
                                    new ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                            );

                            avatarImg.setScaleType(
                                    ImageView.ScaleType.CENTER_INSIDE
                            );

                            avatarImg.setPadding(
                                    (int) dpToPx(4),
                                    (int) dpToPx(4),
                                    (int) dpToPx(4),
                                    (int) dpToPx(4)
                            );

                            if (avatarUrl != null &&
                                    !avatarUrl.isEmpty()) {

                                loadImageAsync(
                                        avatarImg,
                                        avatarUrl
                                );
                            }

                            avatarCard.addView(
                                    avatarImg
                            );

                            avatarCard.setOnClickListener(
                                    new View.OnClickListener() {

                                        @Override
                                        public void onClick(View v) {

                                            OnAvatarClick(
                                                    senderUid,
                                                    isMe
                                            );
                                        }
                                    }
                            );

                            LinearLayout bubble =
                                    new LinearLayout(context);

                            bubble.setOrientation(
                                    LinearLayout.VERTICAL
                            );

                            bubble.setPadding(
                                    (int) dpToPx(16),
                                    (int) dpToPx(10),
                                    (int) dpToPx(16),
                                    (int) dpToPx(10)
                            );

                            GradientDrawable bg =
                                    new GradientDrawable();

                            bg.setShape(
                                    GradientDrawable.RECTANGLE
                            );

                            bg.setColor(
                                    bubbleColor
                            );

                            bg.setCornerRadius(
                                    dpToPx(22)
                            );

                            bubble.setBackground(bg);

                            int maxBubbleWidth =
                                    (int)
                                            (screenWidth * 0.72);

                            LinearLayout.LayoutParams bubbleParams =
                                    new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.WRAP_CONTENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                    );

                            bubble.setLayoutParams(
                                    bubbleParams
                            );

                            TextView msgTv =
                                    new TextView(context);

                            msgTv.setText(
                                    messageText
                            );

                            msgTv.setTextColor(
                                    textColor
                            );

                            msgTv.setTextSize(15);

                            msgTv.setMaxWidth(
                                    maxBubbleWidth
                            );

                            msgTv.setLayoutParams(
                                    new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.WRAP_CONTENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                    )
                            );

                            if (customTypeface != null) {

                                msgTv.setTypeface(
                                        customTypeface
                                );
                            }

                            bubble.addView(msgTv);

                            if (timeText != null &&
                                    !timeText.isEmpty()) {

                                TextView timeTv =
                                        new TextView(context);

                                timeTv.setText(
                                        timeText
                                );

                                timeTv.setTextColor(
                                        Color.argb(
                                                180,
                                                Color.red(textColor),
                                                Color.green(textColor),
                                                Color.blue(textColor)
                                        )
                                );

                                timeTv.setTextSize(10);

                                LinearLayout.LayoutParams timeParams =
                                        new LinearLayout.LayoutParams(
                                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                                LinearLayout.LayoutParams.WRAP_CONTENT
                                        );

                                timeParams.gravity =
                                        Gravity.END;

                                timeParams.setMargins(
                                        0,
                                        (int) dpToPx(2),
                                        0,
                                        0
                                );

                                timeTv.setLayoutParams(
                                        timeParams
                                );

                                if (customTypeface != null) {

                                    timeTv.setTypeface(
                                            customTypeface
                                    );
                                }

                                bubble.addView(
                                        timeTv
                                );
                            }

                            if (isMe) {

                                row.addView(bubble);
                                row.addView(avatarCard);

                            } else {

                                row.addView(avatarCard);
                                row.addView(bubble);
                            }

                            targetLayout.addView(row);

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

    @SimpleFunction(
            description = "Fait défiler le ScrollArrangement jusqu'au tout dernier message."
    )
    public void ScrollToBottom(
            final AndroidViewComponent scrollContainer) {

        activity.runOnUiThread(
                new Runnable() {

                    @Override
                    public void run() {

                        View view =
                                scrollContainer.getView();

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
                        }
                    }
                }
        );
    }

    // =========================================================================
    // 2. SAISIE FLOTTANTE & CLAVIER
    // =========================================================================

    @SimpleFunction(
            description = "Attache la zone de saisie au-dessus du clavier de manière flottante."
    )
    public void AttachFloatingInputWithDynamicHeight(
            final Object inputContainer,
            final Object editTextComponent,
            final int maxHeightPx) {

        if (!(inputContainer instanceof AndroidViewComponent))
            return;

        final View containerView =
                ((AndroidViewComponent)
                        inputContainer)
                        .getView();

        if (containerView == null)
            return;

        final View rootView =
                activity.getWindow()
                        .getDecorView()
                        .getRootView();

        rootView.getViewTreeObserver()
                .addOnGlobalLayoutListener(
                        new ViewTreeObserver.OnGlobalLayoutListener() {

                            @Override
                            public void onGlobalLayout() {

                                Rect r = new Rect();

                                rootView
                                        .getWindowVisibleDisplayFrame(
                                                r
                                        );

                                int screenHeight =
                                        rootView
                                                .getRootView()
                                                .getHeight();

                                int keypadHeight =
                                        screenHeight -
                                                r.bottom;

                                if (keypadHeight >
                                        screenHeight * 0.15) {

                                    containerView
                                            .setTranslationY(
                                                    -keypadHeight
                                            );

                                } else {

                                    containerView
                                            .setTranslationY(0);
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
                editTextComponent == null)
            return;

        final View containerView =
                cardContainer.getView();

        final View editView =
                editTextComponent.getView();

        if (containerView == null ||
                !(editView instanceof EditText))
            return;

        ((EditText) editView)
                .addTextChangedListener(
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

                                containerView
                                        .requestLayout();

                                View parent =
                                        (View)
                                                containerView
                                                        .getParent();

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

    @SimpleFunction(
            description = "Construit la grille de produits depuis un JSON sans élévation."
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
                                    new JSONArray(jsonData);

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

                                                ViewGroup targetLayout =
                                                        getRealLayout(
                                                                scrollContainer
                                                        );

                                                if (targetLayout == null)
                                                    return;

                                                targetLayout
                                                        .removeAllViews();

                                                int cardWidth =
                                                        (int)
                                                                (
                                                                        screenWidth *
                                                                                0.44
                                                                );

                                                int cardHeight =
                                                        (int)
                                                                (
                                                                        screenHeight *
                                                                                0.28
                                                                );

                                                LinearLayout currentRow =
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
                                                                    String.valueOf(i)
                                                            );

                                                    String imageStr =
                                                            item.optString(
                                                                    "image",
                                                                    ""
                                                            );

                                                    String titleStr =
                                                            item.optString(
                                                                    "title",
                                                                    ""
                                                            );

                                                    String priceStr =
                                                            item.optString(
                                                                    "price",
                                                                    ""
                                                            );

                                                    final String productJson =
                                                            item.toString();

                                                    if (i % 2 == 0) {

                                                        currentRow =
                                                                new LinearLayout(
                                                                        context
                                                                );

                                                        currentRow
                                                                .setOrientation(
                                                                        LinearLayout.HORIZONTAL
                                                                );

                                                        currentRow
                                                                .setGravity(
                                                                        Gravity.CENTER_HORIZONTAL
                                                                );

                                                        LinearLayout.LayoutParams rowParams =
                                                                new LinearLayout.LayoutParams(
                                                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                                                );

                                                        rowParams.setMargins(
                                                                0,
                                                                8,
                                                                0,
                                                                8
                                                        );

                                                        currentRow
                                                                .setLayoutParams(
                                                                        rowParams
                                                                );

                                                        targetLayout
                                                                .addView(
                                                                        currentRow
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
                                                            10,
                                                            8,
                                                            10,
                                                            8
                                                    );

                                                    card.setLayoutParams(
                                                            cardParams
                                                    );

                                                    card.setRadius(20f);

                                                    card.setCardBackgroundColor(
                                                            Color.WHITE
                                                    );

                                                    card.setCardElevation(
                                                            0f
                                                    );

                                                    card.setMaxCardElevation(
                                                            0f
                                                    );

                                                    if (Build.VERSION.SDK_INT >=
                                                            Build.VERSION_CODES.LOLLIPOP) {

                                                        card.setOutlineProvider(
                                                                null
                                                        );
                                                    }

                                                    LinearLayout inner =
                                                            new LinearLayout(
                                                                    context
                                                            );

                                                    inner.setOrientation(
                                                            LinearLayout.VERTICAL
                                                    );

                                                    inner.setBackgroundColor(
                                                            Color.WHITE
                                                    );

                                                    inner.setLayoutParams(
                                                            new LinearLayout.LayoutParams(
                                                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                                                    LinearLayout.LayoutParams.MATCH_PARENT
                                                            )
                                                    );

                                                    ImageView img =
                                                            new ImageView(
                                                                    context
                                                            );

                                                    LinearLayout.LayoutParams imgParams =
                                                            new LinearLayout.LayoutParams(
                                                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                                                    0,
                                                                    1.0f
                                                            );

                                                    img.setLayoutParams(
                                                            imgParams
                                                    );

                                                    img.setScaleType(
                                                            ImageView.ScaleType.CENTER_CROP
                                                    );

                                                    img.setBackgroundColor(
                                                            Color.parseColor(
                                                                    "#F5F5F5"
                                                            )
                                                    );

                                                    loadImageAsync(
                                                            img,
                                                            imageStr
                                                    );

                                                    inner.addView(img);

                                                    TextView titleTv =
                                                            new TextView(
                                                                    context
                                                            );

                                                    titleTv.setText(
                                                            titleStr
                                                    );

                                                    titleTv.setTextColor(
                                                            Color.BLACK
                                                    );

                                                    titleTv.setTextSize(
                                                            13
                                                    );

                                                    titleTv.setMaxLines(
                                                            2
                                                    );

                                                    titleTv.setPadding(
                                                            14,
                                                            8,
                                                            14,
                                                            0
                                                    );

                                                    if (customTypeface != null) {

                                                        titleTv.setTypeface(
                                                                customTypeface
                                                        );
                                                    }

                                                    inner.addView(
                                                            titleTv
                                                    );

                                                    TextView priceTv =
                                                            new TextView(
                                                                    context
                                                            );

                                                    priceTv.setText(
                                                            priceStr
                                                    );

                                                    priceTv.setTextColor(
                                                            Color.BLACK
                                                    );

                                                    priceTv.setTextSize(
                                                            14
                                                    );

                                                    priceTv.setTypeface(
                                                            null,
                                                            Typeface.BOLD
                                                    );

                                                    priceTv.setPadding(
                                                            14,
                                                            2,
                                                            14,
                                                            12
                                                    );

                                                    if (customTypeface != null) {

                                                        priceTv.setTypeface(
                                                                customTypeface,
                                                                Typeface.BOLD
                                                        );
                                                    }

                                                    inner.addView(
                                                            priceTv
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
                                                                            uid,
                                                                            productJson
                                                                    );
                                                                }
                                                            }
                                                    );

                                                    if (currentRow != null) {

                                                        currentRow.addView(
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

    // =========================================================================
    // 3B. LISTE DYNAMIQUE DE CATÉGORIES
    // =========================================================================

    @SimpleFunction(
            description = "Génère la liste des catégories/sous-catégories depuis un JSON."
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

                                                if (target == null)
                                                    return;

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
                                                            ">  " +
                                                                    categoryName
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
                                                            (int) dpToPx(16),
                                                            0,
                                                            (int) dpToPx(8)
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
                                                             
