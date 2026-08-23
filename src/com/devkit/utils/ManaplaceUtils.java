package com.devkit.utils;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.runtime.util.MediaUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@DesignerComponent(
        version = 1,
        description = "Extension ManaplaceUtils - Barre de navigation flottante et saisie flottante au-dessus du clavier.",
        category = ComponentCategory.EXTENSION,
        nonVisible = true
)
@SimpleObject(external = true)
public class ManaplaceUtils extends AndroidNonvisibleComponent {

    private final Context context;
    private final Activity activity;
    private final Form monForm;

    // =========================================================================
    // ÉTAT DE LA BARRE DE NAVIGATION
    // =========================================================================

    private boolean dejaInitialise = false;

    private int tailleIconeDp = 26;

    private final List<String> idsEnAttente = new ArrayList<>();
    private final List<String> iconesEnAttente = new ArrayList<>();

    private final List<ImageView> vuesIcones = new ArrayList<>();
    private final List<View> vuesCercles = new ArrayList<>();
    private final List<String> idsFinaux = new ArrayList<>();

    private String idSelectionne = null;

    // =========================================================================
    // CONSTRUCTEUR
    // =========================================================================

    public ManaplaceUtils(ComponentContainer container) {
        super(container.$form());

        this.context = container.$context();
        this.activity = (Activity) container.$context();
        this.monForm = container.$form();
    }

    // =========================================================================
    // 1. BARRE DE NAVIGATION FLOTTANTE
    // =========================================================================

    @SimpleFunction(
            description = "Ajoute une icône à la barre de navigation. "
                    + "À appeler une fois par icône, avant NavBarInitialize."
    )
    public void NavBarAdd(String id, String icon) {

        if (id == null || id.isEmpty()) {
            NavBarError("L'id de l'icône est vide.");
            return;
        }

        if (idsEnAttente.contains(id)) {
            NavBarError("Id déjà utilisé: " + id);
            return;
        }

        idsEnAttente.add(id);
        iconesEnAttente.add(icon);
    }

    // =========================================================================

    @SimpleFunction(
            description = "Construit et affiche la barre flottante avec "
                    + "toutes les icônes ajoutées via NavBarAdd."
    )
    public void NavBarInitialize(
            final int margeBas,
            final double largeurPourcent,
            final double hauteurPourcent) {

        if (dejaInitialise) {
            return;
        }

        if (idsEnAttente.isEmpty()) {
            NavBarError(
                    "Aucune icône ajoutée — appelle NavBarAdd avant NavBarInitialize"
            );
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                try {

                    FrameLayout root =
                            (FrameLayout) activity.findViewById(
                                    android.R.id.content
                            );

                    if (root == null) {
                        NavBarError("Écran racine introuvable");
                        return;
                    }

                    DisplayMetrics metrics =
                            activity.getResources().getDisplayMetrics();

                    int largeurFinale;

                    if (largeurPourcent > 0) {
                        largeurFinale =
                                (int) (
                                        metrics.widthPixels
                                                * (largeurPourcent / 100.0)
                                );
                    } else {
                        largeurFinale =
                                ViewGroup.LayoutParams.WRAP_CONTENT;
                    }

                    int hauteurFinale;

                    if (hauteurPourcent > 0) {
                        hauteurFinale =
                                (int) (
                                        metrics.heightPixels
                                                * (hauteurPourcent / 100.0)
                                );
                    } else {
                        hauteurFinale =
                                (int) dpToPx(64);
                    }

                    // ---------------------------------------------------------
                    // BARRE
                    // ---------------------------------------------------------

                    LinearLayout bar =
                            new LinearLayout(activity);

                    bar.setOrientation(LinearLayout.HORIZONTAL);
                    bar.setGravity(Gravity.CENTER);
                    bar.setWeightSum(idsEnAttente.size());

                    GradientDrawable fond =
                            new GradientDrawable();

                    fond.setColor(Color.WHITE);
                    fond.setCornerRadius(dpToPx(30));

                    bar.setBackground(fond);
                    bar.setElevation(dpToPx(12));

                    // ---------------------------------------------------------
                    // CRÉATION DES ICÔNES
                    // ---------------------------------------------------------

                    for (int i = 0;
                         i < idsEnAttente.size();
                         i++) {

                        final String tabId =
                                idsEnAttente.get(i);

                        String iconFile =
                                iconesEnAttente.get(i);

                        // -----------------------------------------------------
                        // CONTENEUR DE L'ICÔNE
                        // -----------------------------------------------------

                        FrameLayout conteneur =
                                new FrameLayout(activity);

                        LinearLayout.LayoutParams
                                pConteneur =
                                new LinearLayout.LayoutParams(
                                        0,
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        1f
                                );

                        conteneur.setLayoutParams(pConteneur);

                        // -----------------------------------------------------
                        // CERCLE DE SÉLECTION
                        // -----------------------------------------------------

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

                        FrameLayout.LayoutParams
                                pCercle =
                                new FrameLayout.LayoutParams(
                                        (int) dpToPx(46),
                                        (int) dpToPx(46),
                                        Gravity.CENTER
                                );

                        conteneur.addView(
                                cercle,
                                pCercle
                        );

                        // -----------------------------------------------------
                        // IMAGE
                        // -----------------------------------------------------

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
                                    "Icône introuvable: "
                                            + iconFile
                            );
                        }

                        int taillePx =
                                (int) dpToPx(
                                        tailleIconeDp
                                );

                        FrameLayout.LayoutParams
                                pImg =
                                new FrameLayout.LayoutParams(
                                        taillePx,
                                        taillePx,
                                        Gravity.CENTER
                                );

                        conteneur.addView(
                                img,
                                pImg
                        );

                        // -----------------------------------------------------
                        // CLICK
                        // -----------------------------------------------------

                        final View cercleFinal =
                                cercle;

                        final ImageView imgFinal =
                                img;

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

                    // ---------------------------------------------------------
                    // POSITION DE LA BARRE
                    // ---------------------------------------------------------

                    FrameLayout.LayoutParams params =
                            new FrameLayout.LayoutParams(
                                    largeurFinale,
                                    hauteurFinale
                            );

                    params.gravity =
                            Gravity.BOTTOM
                                    | Gravity.CENTER_HORIZONTAL;

                    params.setMargins(
                            0,
                            0,
                            0,
                            (int) dpToPx(margeBas)
                    );

                    root.addView(
                            bar,
                            params
                    );

                    dejaInitialise = true;

                } catch (Exception e) {

                    NavBarError(
                            "Erreur inattendue: "
                                    + e.getMessage()
                    );
                }
            }
        });
    }

    // =========================================================================

    @SimpleFunction(
            description = "Ajuste la taille de toutes les icônes "
                    + "de la barre en dp avec une transition animée."
    )
    public void NavBarSetIconSize(final int tailleDp) {

        if (tailleDp <= 0) {
            NavBarError(
                    "La taille de l'icône doit être supérieure à 0."
            );
            return;
        }

        final int ancienneTailleDp =
                tailleIconeDp;

        tailleIconeDp =
                tailleDp;

        if (vuesIcones.isEmpty()) {
            return;
        }

        try {

            final float ancienPx =
                    dpToPx(ancienneTailleDp);

            final float nouveauPx =
                    dpToPx(tailleDp);

            ValueAnimator anim =
                    ValueAnimator.ofFloat(
                            ancienPx,
                            nouveauPx
                    );

            anim.setDuration(220);

            anim.addUpdateListener(
                    new ValueAnimator.AnimatorUpdateListener() {

                        @Override
                        public void onAnimationUpdate(
                                ValueAnimator animation) {

                            int taillePx =
                                    (int) (
                                            floatValue(
                                                    animation.getAnimatedValue()
                                            )
                                    );

                            for (ImageView iv :
                                    vuesIcones) {

                                ViewGroup.LayoutParams p =
                                        iv.getLayoutParams();

                                p.width =
                                        taillePx;

                                p.height =
                                        taillePx;

                                iv.setLayoutParams(p);
                            }
                        }
                    }
            );

            anim.start();

        } catch (Exception e) {

            NavBarError(
                    "Erreur NavBarSetIconSize: "
                            + e.getMessage()
            );
        }
    }

    // =========================================================================

    @SimpleFunction(
            description = "Sélectionne un onglet de la barre par son ID, "
                    + "sans effectuer de clic."
    )
    public void NavBarSelect(String id) {

        int index =
                idsFinaux.indexOf(id);

        if (index < 0) {

            NavBarError(
                    "Id introuvable pour NavBarSelect: "
                            + id
            );

            return;
        }

        SelectionnerOnglet(
                id,
                vuesCercles.get(index),
                vuesIcones.get(index)
        );
    }

    // =========================================================================
    // SÉLECTION D'UN ONGLET
    // =========================================================================

    private void SelectionnerOnglet(
            String id,
            View cercle,
            ImageView img) {

        try {

            if (id.equals(idSelectionne)) {
                return;
            }

            // -------------------------------------------------------------
            // Désélection de l'ancien onglet
            // -------------------------------------------------------------

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

            // -------------------------------------------------------------
            // Sélection du nouvel onglet
            // -------------------------------------------------------------

            animerOnglet(
                    cercle,
                    img,
                    true
            );

            idSelectionne =
                    id;

            OnSelected(id);

        } catch (Exception e) {

            NavBarError(
                    "Erreur de sélection: "
                            + e.getMessage()
            );
        }
    }

    // =========================================================================
    // ANIMATION DE SÉLECTION
    // =========================================================================

    private void animerOnglet(
            final View cercle,
            final ImageView img,
            final boolean selectionne) {

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

        anim.addUpdateListener(
                new ValueAnimator.AnimatorUpdateListener() {

                    @Override
                    public void onAnimationUpdate(
                            ValueAnimator animation) {

                        float val =
                                floatValue(
                                        animation.getAnimatedValue()
                                );

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

    // =========================================================================
    // MÉLANGE DE COULEURS
    // =========================================================================

    private int melangerCouleurs(
            int c1,
            int c2,
            float ratio) {

        int r =
                (int) (
                        Color.red(c1)
                                + ratio
                                * (
                                Color.red(c2)
                                        - Color.red(c1)
                        )
                );

        int g =
                (int) (
                        Color.green(c1)
                                + ratio
                                * (
                                Color.green(c2)
                                        - Color.green(c1)
                        )
                );

        int b =
                (int) (
                        Color.blue(c1)
                                + ratio
                                * (
                                Color.blue(c2)
                                        - Color.blue(c1)
                        )
                );

        return Color.rgb(
                r,
                g,
                b
        );
    }

    // =========================================================================
    // DP -> PX
    // =========================================================================

    private float dpToPx(int dp) {

        float density =
                activity
                        .getResources()
                        .getDisplayMetrics()
                        .density;

        return dp * density;
    }

    // =========================================================================
    // UTILITAIRE FLOAT
    // =========================================================================

    private float floatValue(Object value) {

        if (value instanceof Float) {
            return (Float) value;
        }

        if (value instanceof Double) {
            return ((Double) value).floatValue();
        }

        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }

        return 0f;
    }

    // =========================================================================
    // 2. SAISIE FLOTTANTE AU-DESSUS DU CLAVIER
    // =========================================================================

    @SimpleFunction(
            description = "Attache la zone de saisie au-dessus du clavier "
                    + "et ajuste automatiquement sa position."
    )
    public void AttachFloatingInputWithDynamicHeight(
            final AndroidViewComponent inputContainer,
            final AndroidViewComponent editTextComponent,
            final int maxHeightPx) {

        final View containerView =
                inputContainer.getView();

        final View rootView =
                activity
                        .getWindow()
                        .getDecorView()
                        .getRootView();

        rootView
                .getViewTreeObserver()
                .addOnGlobalLayoutListener(
                        new ViewTreeObserver.OnGlobalLayoutListener() {

                            @Override
                            public void onGlobalLayout() {

                                try {

                                    Rect r =
                                            new Rect();

                                    rootView
                                            .getWindowVisibleDisplayFrame(
                                                    r
                                            );

                                    int screenHeight =
                                            rootView
                                                    .getRootView()
                                                    .getHeight();

                                    int keypadHeight =
                                            screenHeight
                                                    - r.bottom;

                                    if (keypadHeight
                                            > screenHeight * 0.15) {

                                        containerView
                                                .setTranslationY(
                                                        -keypadHeight
                                                );

                                    } else {

                                        containerView
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

    // =========================================================================
    // 3. ÉVÉNEMENTS KODULAR
    // =========================================================================

    @SimpleEvent(
            description = "Déclenché quand l'utilisateur touche "
                    + "une icône de la barre de navigation."
    )
    public void OnSelected(String id) {

        EventDispatcher.dispatchEvent(
                this,
                "OnSelected",
                id
        );
    }

    // =========================================================================

    @SimpleEvent(
            description = "Déclenché en cas de problème "
                    + "avec la barre de navigation."
    )
    public void NavBarError(String message) {

        EventDispatcher.dispatchEvent(
                this,
                "NavBarError",
                message
        );
    }
}
