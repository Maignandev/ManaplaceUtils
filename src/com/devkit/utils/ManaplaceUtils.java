Package com.devkit.utils;

Import android.animation.ValueAnimator;
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

Import androidx.cardview.widget.CardView;

Import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.MediaUtil;

Import org.json.JSONArray;
import org.json.JSONObject;

Import java.io.BufferedReader;
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
        Version = 10,
        Description = "Extension ManaplaceUtils mise à jour pour Kodular.",
        Category = ComponentCategory.EXTENSION,
        NonVisible = true
)
@SimpleObject(external = true)
@UsesPermissions(
        PermissionNames =
                "android.permission.READ_EXTERNAL_STORAGE," +
                "android.permission.READ_MEDIA_IMAGES," +
                "android.permission.INTERNET"
)
public class ManaplaceUtils extends AndroidNonvisibleComponent implements ActivityResultListener {

    Private final Context context;
    Private final Activity activity;
    Private final Form monForm;
    Private Dialog activeAlphaDialog;
    Private final int PICK_IMAGE_REQUEST = 1001;

    Private Typeface customTypeface = Typeface.DEFAULT;
    Private int radioButtonColor = Color.parseColor("#C01A1A1B");

    // =========================================================================
    // 0. BARRE DE NAVIGATION FLOTTANTE
    // =========================================================================

    Private boolean dejaInitialise = false;
    Private int tailleIconeDp = 26;
    Private final List<String> idsEnAttente = new ArrayList<>();
    Private final List<String> iconesEnAttente = new ArrayList<>();
    Private final List<ImageView> vuesIcones = new ArrayList<>();
    Private final List<View> vuesCercles = new ArrayList<>();
    Private final List<String> idsFinaux = new ArrayList<>();
    Private String idSelectionne = null;

    Public ManaplaceUtils(ComponentContainer container) {
        Super(container.$form());
        This.context = container.$context();
        This.activity = (Activity) container.$context();
        This.monForm = container.$form();
        This.form.registerForActivityResult(this);
    }

    Private float dpToPx(int dp) {
        Return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                Dp,
                Context.getResources().getDisplayMetrics()
        );
    }

    @SimpleFunction(description = "Ajoute une icône à la barre de navigation. À appeler une fois par icône, avant NavBarInitialize.")
    Public void NavBarAdd(String id, String icon) {
        If (idsEnAttente.contains(id)) {
            NavBarError("Id déjà utilisé: " + id);
            Return;
        }

        IdsEnAttente.add(id);
        IconesEnAttente.add(icon);
    }

    @SimpleFunction(description = "Construit et affiche la barre flottante avec toutes les icônes ajoutées via NavBarAdd.")
    Public void NavBarInitialize(final int margeBas, final double largeurPourcent, final double hauteurPourcent) {
        If (dejaInitialise) return;

        If (idsEnAttente.isEmpty()) {
            NavBarError("Aucune icône ajoutée — appelle NavBarAdd avant NavBarInitialize");
            Return;
        }

        Activity.runOnUiThread(new Runnable() {
            @Override
            Public void run() {
                Try {
                    Final FrameLayout root = (FrameLayout) activity.findViewById(android.R.id.content);

                    If (root == null) {
                        NavBarError("Écran racine introuvable");
                        Return;
                    }

                    // Attendre l'initialisation complète du layout (compatible Screen.Initialize)
                    Root.post(new Runnable() {
                        @Override
                        Public void run() {
                            Try {
                                DisplayMetrics metrics = activity.getResources().getDisplayMetrics();

                                Int largeurFinale = (largeurPourcent > 0)
                                        ? (int) (metrics.widthPixels * (largeurPourcent / 100.0))
                                        : ViewGroup.LayoutParams.WRAP_CONTENT;

                                Int hauteurFinale = (hauteurPourcent > 0)
                                        ? (int) (metrics.heightPixels * (hauteurPourcent / 100.0))
                                        : (int) dpToPx(64);

                                LinearLayout bar = new LinearLayout(activity);
                                Bar.setOrientation(LinearLayout.HORIZONTAL);
                                Bar.setGravity(Gravity.CENTER);
                                Bar.setWeightSum(idsEnAttente.size());

                                GradientDrawable fond = new GradientDrawable();
                                Fond.setColor(Color.WHITE);
                                Fond.setCornerRadius(dpToPx(30));
                                Bar.setBackground(fond);

                                // Élévation supprimée selon la demande

                                For (int i = 0; i < idsEnAttente.size(); i++) {
                                    Final String tabId = idsEnAttente.get(i);
                                    String iconFile = iconesEnAttente.get(i);

                                    FrameLayout conteneur = new FrameLayout(activity);
                                    LinearLayout.LayoutParams pConteneur = new LinearLayout.LayoutParams(
                                            0,
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            1f
                                    );
                                    Conteneur.setLayoutParams(pConteneur);

                                    View cercle = new View(activity);
                                    GradientDrawable fondCercle = new GradientDrawable();
                                    FondCercle.setShape(GradientDrawable.OVAL);
                                    FondCercle.setColor(Color.argb(30, 0, 0, 0));
                                    Cercle.setBackground(fondCercle);
                                    Cercle.setAlpha(0f);

                                    FrameLayout.LayoutParams pCercle = new FrameLayout.LayoutParams(
                                            (int) dpToPx(46),
                                            (int) dpToPx(46),
                                            Gravity.CENTER
                                    );
                                    Conteneur.addView(cercle, pCercle);

                                    ImageView img = new ImageView(activity);
                                    Img.setAdjustViewBounds(true);

                                    Try {
                                        Drawable d = MediaUtil.getBitmapDrawable(monForm, iconFile);
                                        Img.setImageDrawable(d);
                                        Img.setColorFilter(
                                                New PorterDuffColorFilter(
                                                        Color.rgb(150, 150, 150),
                                                        PorterDuff.Mode.SRC_IN
                                                )
                                        );
                                    } catch (IOException e) {
                                        NavBarError("Icône introuvable: " + iconFile);
                                    }

                                    Int taillePx = (int) dpToPx(tailleIconeDp);
                                    FrameLayout.LayoutParams pImg = new FrameLayout.LayoutParams(
                                            TaillePx,
                                            TaillePx,
                                            Gravity.CENTER
                                    );
                                    Conteneur.addView(img, pImg);

                                    Final View cercleFinal = cercle;
                                    Final ImageView imgFinal = img;

                                    Conteneur.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        Public void onClick(View v) {
                                            SelectionnerOnglet(tabId, cercleFinal, imgFinal);
                                        }
                                    });

                                    VuesIcones.add(img);
                                    VuesCercles.add(cercle);
                                    IdsFinaux.add(tabId);

                                    Bar.addView(conteneur);
                                }

                                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                                        LargeurFinale,
                                        HauteurFinale
                                );
                                Params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                                Params.setMargins(0, 0, 0, (int) dpToPx(margeBas));

                                Root.addView(bar, params);
                                DejaInitialise = true;

                            } catch (Exception e) {
                                NavBarError("Erreur initialisation bar: " + e.getMessage());
                            }
                        }
                    });

                } catch (Exception e) {
                    NavBarError("Erreur inattendue: " + e.getMessage());
                }
            }
        });
    }

    @SimpleFunction(description = "Ajuste la taille de toutes les icônes de la barre en dp, avec une transition animée.")
    Public void NavBarSetIconSize(final int tailleDp) {
        Final int ancienneTailleDp = tailleIconeDp;
        TailleIconeDp = tailleDp;

        If (vuesIcones.isEmpty()) return;

        Try {
            Final float ancienPx = dpToPx(ancienneTailleDp);
            Final float nouveauPx = dpToPx(tailleDp);

            ValueAnimator anim = ValueAnimator.ofFloat(ancienPx, nouveauPx);
            Anim.setDuration(220);
            Anim.setInterpolator(new DecelerateInterpolator());

            Anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                Public void onAnimationUpdate(ValueAnimator animation) {
                    Int taillePx = (int) (float) animation.getAnimatedValue();
                    For (ImageView iv : vuesIcones) {
                        ViewGroup.LayoutParams p = iv.getLayoutParams();
                        P.width = taillePx;
                        P.height = taillePx;
                        Iv.setLayoutParams(p);
                    }
                }
            });

            Anim.start();
        } catch (Exception e) {
            NavBarError("Erreur NavBarSetIconSize: " + e.getMessage());
        }
    }

    @SimpleFunction(description = "Sélectionne un onglet de la barre par code, sans clic.")
    Public void NavBarSelect(String id) {
        Int index = idsFinaux.indexOf(id);

        If (index < 0) {
            NavBarError("Id introuvable pour NavBarSelect: " + id);
            Return;
        }

        SelectionnerOnglet(id, vuesCercles.get(index), vuesIcones.get(index));
    }

    Private void SelectionnerOnglet(String id, View cercle, ImageView img) {
        Try {
            If (id.equals(idSelectionne)) return;

            If (idSelectionne != null) {
                Int ancienIndex = idsFinaux.indexOf(idSelectionne);
                If (ancienIndex >= 0) {
                    AnimerOnglet(vuesCercles.get(ancienIndex), vuesIcones.get(ancienIndex), false);
                }
            }

            AnimerOnglet(cercle, img, true);
            IdSelectionne = id;
            OnSelected(id);

        } catch (Exception e) {
            NavBarError("Erreur de sélection: " + e.getMessage());
        }
    }

    Private void animerOnglet(final View cercle, final ImageView img, boolean selectionne) {
        Float alphaCible = selectionne ? 1f : 0f;

        ValueAnimator anim = ValueAnimator.ofFloat(cercle.getAlpha(), alphaCible);
        Anim.setDuration(220);
        Anim.setInterpolator(new DecelerateInterpolator());

        Anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            Public void onAnimationUpdate(ValueAnimator animation) {
                Float val = (float) animation.getAnimatedValue();
                Cercle.setAlpha(val);

                Int couleur = melangerCouleurs(
                        Color.rgb(150, 150, 150),
                        Color.rgb(20, 20, 20),
                        Val
                );

                Img.setColorFilter(new PorterDuffColorFilter(couleur, PorterDuff.Mode.SRC_IN));
            }
        });

        Anim.start();
    }

    Private int melangerCouleurs(int c1, int c2, float ratio) {
        Int r = (int) (Color.red(c1) + ratio * (Color.red(c2) - Color.red(c1)));
        Int g = (int) (Color.green(c1) + ratio * (Color.green(c2) - Color.green(c1)));
        Int b = (int) (Color.blue(c1) + ratio * (Color.blue(c2) - Color.blue(c1)));
        Return Color.rgb(r, g, b);
    }

    // =========================================================================
    // POLICE PERSONNALISÉE
    // =========================================================================

    @SimpleFunction(description = "Charge une police personnalisée .ttf ou .otf.")
    Public void LoadCustomFont(String fontPath) {
        Try {
            If (fontPath == null || fontPath.trim().isEmpty()) {
                CustomTypeface = Typeface.DEFAULT;
                Return;
            }

            If (fontPath.startsWith("/")) {
                CustomTypeface = Typeface.createFromFile(new File(fontPath));
            } else {
                CustomTypeface = Typeface.createFromAsset(context.getAssets(), fontPath);
            }
        } catch (Exception e) {
            E.printStackTrace();
            CustomTypeface = Typeface.DEFAULT;
        }
    }

    @SimpleFunction(description = "Définit la couleur des boutons radio.")
    Public void SetRadioButtonColor(int color) {
        RadioButtonColor = color;
    }

    // =========================================================================
    // UTILITAIRES IMAGES
    // =========================================================================

    Private ViewGroup getRealLayout(AndroidViewComponent component) {
        If (component == null) return null;
        View view = component.getView();

        If (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            If (vg.getChildCount() > 0 && vg.getChildAt(0) instanceof ViewGroup) {
                Return (ViewGroup) vg.getChildAt(0);
            }
            Return vg;
        }
        Return null;
    }

    Private void runOnUi(Runnable runnable) {
        Activity.runOnUiThread(runnable);
    }

    Private void loadImageAsync(final ImageView imageView, final String imagePath) {
        If (imagePath == null || imagePath.trim().isEmpty()) return;

        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            Public void run() {
                Bitmap bmp = null;
                InputStream input = null;
                HttpURLConnection conn = null;

                Try {
                    If (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                        URL url = new URL(imagePath);
                        Conn = (HttpURLConnection) url.openConnection();
                        Conn.setConnectTimeout(15000);
                        Conn.setReadTimeout(15000);
                        Conn.setDoInput(true);
                        Conn.connect();
                        Input = conn.getInputStream();
                        Bmp = BitmapFactory.decodeStream(input);

                    } else if (imagePath.startsWith("content://")) {
                        Input = context.getContentResolver().openInputStream(Uri.parse(imagePath));
                        If (input != null) {
                            Bmp = BitmapFactory.decodeStream(input);
                        }

                    } else {
                        Try {
                            Input = context.getAssets().open(imagePath);
                            Bmp = BitmapFactory.decodeStream(input);
                        } catch (Exception assetError) {
                            Try {
                                Bmp = MediaUtil.getBitmapDrawable(monForm, imagePath).getBitmap();
                            } catch (Exception mediaError) {
                                File file = new File(imagePath);
                                If (file.exists()) {
                                    Input = new FileInputStream(file);
                                    Bmp = BitmapFactory.decodeStream(input);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    E.printStackTrace();
                } finally {
                    If (input != null) {
                        Try { input.close(); } catch (Exception ignored) {}
                    }
                    If (conn != null) {
                        Conn.disconnect();
                    }
                }

                Final Bitmap finalBmp = bmp;
                If (finalBmp != null) {
                    Activity.runOnUiThread(new Runnable() {
                        @Override
                        Public void run() {
                            If (imageView.getWindowToken() != null || imageView.isAttachedToWindow()) {
                                ImageView.setImageBitmap(finalBmp);
                            }
                        }
                    });
                }
            }
        });
    }

    // =========================================================================
    // 1. MOTEUR DE CHAT DYNAMIQUE NATIVE
    // =========================================================================

    @SimpleFunction(description = "Ajoute une bulle de chat avec un petit avatar rond.")
    Public void AddChatBubble(
            Final AndroidViewComponent chatContainer,
            Final String messageText,
            Final String timeText,
            Final String avatarUrl,
            Final boolean isMe,
            Final int bubbleColor,
            Final int textColor) {

        RunOnUi(new Runnable() {
            @Override
            Public void run() {
                Try {
                    ViewGroup targetLayout = getRealLayout(chatContainer);
                    If (targetLayout == null) return;

                    Int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;

                    LinearLayout row = new LinearLayout(context);
                    Row.setOrientation(LinearLayout.HORIZONTAL);
                    Row.setGravity(isMe ? Gravity.END : Gravity.START);

                    LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    RowParams.setMargins((int) dpToPx(8), (int) dpToPx(4), (int) dpToPx(8), (int) dpToPx(4));
                    Row.setLayoutParams(rowParams);

                    Int avatarSizePx = (int) dpToPx(32);
                    CardView avatarCard = new CardView(context);
                    LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(avatarSizePx, avatarSizePx);
                    AvatarParams.gravity = Gravity.CENTER_VERTICAL;
                    AvatarParams.setMargins((int) dpToPx(6), 0, (int) dpToPx(6), 0);
                    AvatarCard.setLayoutParams(avatarParams);
                    AvatarCard.setRadius(avatarSizePx / 2f);
                    AvatarCard.setCardElevation(0f);
                    AvatarCard.setMaxCardElevation(0f);
                    AvatarCard.setCardBackgroundColor(Color.parseColor("#E0E0E0"));

                    ImageView avatarImg = new ImageView(context);
                    AvatarImg.setLayoutParams(new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    ));
                    AvatarImg.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    AvatarImg.setPadding((int) dpToPx(4), (int) dpToPx(4), (int) dpToPx(4), (int) dpToPx(4));

                    If (avatarUrl != null && !avatarUrl.isEmpty()) {
                        LoadImageAsync(avatarImg, avatarUrl);
                    }

                    AvatarCard.addView(avatarImg);
                    AvatarCard.setOnClickListener(new View.OnClickListener() {
                        @Override
                        Public void onClick(View v) {
                            OnAvatarClick(isMe);
                        }
                    });

                    LinearLayout bubble = new LinearLayout(context);
                    Bubble.setOrientation(LinearLayout.VERTICAL);
                    Bubble.setPadding((int) dpToPx(16), (int) dpToPx(10), (int) dpToPx(16), (int) dpToPx(10));

                    GradientDrawable bg = new GradientDrawable();
                    Bg.setShape(GradientDrawable.RECTANGLE);
                    Bg.setColor(bubbleColor);
                    Bg.setCornerRadius(dpToPx(22));
                    Bubble.setBackground(bg);

                    Int maxBubbleWidth = (int) (screenWidth * 0.72);
                    LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    Bubble.setLayoutParams(bubbleParams);

                    TextView msgTv = new TextView(context);
                    MsgTv.setText(messageText);
                    MsgTv.setTextColor(textColor);
                    MsgTv.setTextSize(15);
                    MsgTv.setMaxWidth(maxBubbleWidth);
                    MsgTv.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    ));

                    If (customTypeface != null) {
                        MsgTv.setTypeface(customTypeface);
                    }

                    Bubble.addView(msgTv);

                    If (timeText != null && !timeText.isEmpty()) {
                        TextView timeTv = new TextView(context);
                        TimeTv.setText(timeText);
                        TimeTv.setTextColor(Color.argb(
                                180,
                                Color.red(textColor),
                                Color.green(textColor),
                                Color.blue(textColor)
                        ));
                        TimeTv.setTextSize(10);

                        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        TimeParams.gravity = Gravity.END;
                        TimeParams.setMargins(0, (int) dpToPx(2), 0, 0);
                        TimeTv.setLayoutParams(timeParams);

                        If (customTypeface != null) {
                            TimeTv.setTypeface(customTypeface);
                        }

                        Bubble.addView(timeTv);
                    }

                    If (isMe) {
                        Row.addView(bubble);
                        Row.addView(avatarCard);
                    } else {
                        Row.addView(avatarCard);
                        Row.addView(bubble);
                    }

                    TargetLayout.addView(row);
                    ScrollToBottom(chatContainer);

                } catch (Exception e) {
                    E.printStackTrace();
                }
            }
        });
    }

    @SimpleFunction(description = "Fait défiler le ScrollArrangement jusqu'au tout dernier message.")
    Public void ScrollToBottom(final AndroidViewComponent scrollContainer) {
        Activity.runOnUiThread(new Runnable() {
            @Override
            Public void run() {
                View view = scrollContainer.getView();
                If (view instanceof ScrollView) {
                    Final ScrollView scrollView = (ScrollView) view;
                    ScrollView.post(new Runnable() {
                        @Override
                        Public void run() {
                            ScrollView.fullScroll(View.FOCUS_DOWN);
                        }
                    });
                }
            }
        });
    }

    // =========================================================================
    // 2. SAISIE FLOTTANTE & CLAVIER (CORRIGÉ & DYNAMIQUE)
    // =========================================================================

    @SimpleFunction(description = "Attache le conteneur de saisie au clavier et adapte dynamiquement la hauteur de la TextBox selon les lignes tapées.")
    Public void AttachFloatingInputWithDynamicHeight(
            Final AndroidViewComponent inputContainer,
            Final TextBoxBase textBoxComponent,
            Final int maxHeightPx) {

        If (inputContainer == null || textBoxComponent == null) return;

        Final View container = inputContainer.getView();
        View textDbView = textBoxComponent.getView();

        If (container == null || !(textDbView instanceof EditText)) return;

        Final EditText editText = (EditText) textDbView;
        Final View root = activity.getWindow().getDecorView().getRootView();

        // 1. Détection dynamique de l'extension multi-lignes
        EditText.setMaxLines(10); // Autorise la croissance
        EditText.setHorizontallyScrolling(false);

        ViewGroup.LayoutParams params = editText.getLayoutParams();
        Params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        EditText.setLayoutParams(params);

        EditText.addTextChangedListener(new TextWatcher() {
            @Override
            Public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            Public void onTextChanged(CharSequence s, int start, int before, int count) {
                If (maxHeightPx > 0 && editText.getHeight() > maxHeightPx) {
                    ViewGroup.LayoutParams lp = editText.getLayoutParams();
                    Lp.height = maxHeightPx;
                    EditText.setLayoutParams(lp);
                }
            }

            @Override
            Public void afterTextChanged(Editable s) {}
        });

        // 2. Suivi de la position au-dessus du clavier
        Root.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            Public void onGlobalLayout() {
                Rect rect = new Rect();
                Root.getWindowVisibleDisplayFrame(rect);

                Int screenHeight = root.getRootView().getHeight();
                Int keyboardHeight = screenHeight - rect.bottom;

                If (keyboardHeight > screenHeight * 0.15f) {
                    Container.setTranslationY(-keyboardHeight);
                } else {
                    Container.setTranslationY(0);
                }
            }
        });
    }

    // =========================================================================
    // 3. CATALOGUE DE PRODUITS 2x2 NATIVE
    // =========================================================================

    @SimpleFunction(description = "Construit la grille de produits depuis un JSON sans élévation.")
    Public void BuildProductGridFromJson(
            Final AndroidViewComponent scrollContainer,
            Final String jsonData) {

        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            Public void run() {
                Try {
                    Final JSONArray array = new JSONArray(jsonData);
                    Final int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
                    Final int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;

                    RunOnUi(new Runnable() {
                        @Override
                        Public void run() {
                            Try {
                                ViewGroup targetLayout = getRealLayout(scrollContainer);
                                If (targetLayout == null) return;

                                TargetLayout.removeAllViews();

                                Int cardWidth = (int) (screenWidth * 0.44);
                                Int cardHeight = (int) (screenHeight * 0.28);

                                LinearLayout currentRow = null;

                                For (int i = 0; i < array.length(); i++) {
                                    JSONObject item = array.getJSONObject(i);

                                    Final String uid = item.optString("uid", String.valueOf(i));
                                    String imageStr = item.optString("image", "");
                                    String titleStr = item.optString("title", "");
                                    String priceStr = item.optString("price", "");

                                    If (i % 2 == 0) {
                                        CurrentRow = new LinearLayout(context);
                                        CurrentRow.setOrientation(LinearLayout.HORIZONTAL);
                                        CurrentRow.setGravity(Gravity.CENTER_HORIZONTAL);

                                        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                                                LinearLayout.LayoutParams.MATCH_PARENT,
                                                LinearLayout.LayoutParams.WRAP_CONTENT
                                        );
                                        RowParams.setMargins(0, 8, 0, 8);
                                        CurrentRow.setLayoutParams(rowParams);

                                        TargetLayout.addView(currentRow);
                                    }

                                    CardView card = new CardView(context);
                                    LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                                            CardWidth,
                                            CardHeight
                                    );
                                    CardParams.setMargins(10, 8, 10, 8);
                                    Card.setLayoutParams(cardParams);
                                    Card.setRadius(20f);
                                    Card.setCardBackgroundColor(Color.WHITE);
                                    Card.setCardElevation(0f);
                                    Card.setMaxCardElevation(0f);

                                    LinearLayout inner = new LinearLayout(context);
                                    Inner.setOrientation(LinearLayout.VERTICAL);
                                    Inner.setBackgroundColor(Color.WHITE);
                                    Inner.setLayoutParams(new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.MATCH_PARENT
                                    ));

                                    ImageView img = new ImageView(context);
                                    LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            0,
                                            1.0f
                                    );
                                    Img.setLayoutParams(imgParams);
                                    Img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                    Img.setBackgroundColor(Color.parseColor("#F5F5F5"));

                                    LoadImageAsync(img, imageStr);
                                    Inner.addView(img);

                                    TextView titleTv = new TextView(context);
                                    TitleTv.setText(titleStr);
                                    TitleTv.setTextColor(Color.BLACK);
                                    TitleTv.setTextSize(13);
                                    TitleTv.setMaxLines(2);
                                    TitleTv.setPadding(14, 8, 14, 0);

                                    If (customTypeface != null) {
                                        TitleTv.setTypeface(customTypeface);
                                    }
                                    Inner.addView(titleTv);

                                    TextView priceTv = new TextView(context);
                                    PriceTv.setText(priceStr);
                                    PriceTv.setTextColor(Color.BLACK);
                                    PriceTv.setTextSize(14);
                                    PriceTv.setTypeface(null, Typeface.BOLD);
                                    PriceTv.setPadding(14, 2, 14, 12);

                                    If (customTypeface != null) {
                                        PriceTv.setTypeface(customTypeface, Typeface.BOLD);
                                    }
                                    Inner.addView(priceTv);

                                    Card.addView(inner);

                                    Card.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        Public void onClick(View v) {
                                            OnProductCardClick(uid);
                                        }
                                    });

                                    If (currentRow != null) {
                                        CurrentRow.addView(card);
                                    }
                                }

                            } catch (Exception e) {
                                E.printStackTrace();
                            }
                        }
                    });

                } catch (Exception e) {
                    E.printStackTrace();
                }
            }
        });
    }

    // =========================================================================
    // 3B. LISTE DYNAMIQUE DE CATÉGORIES
    // =========================================================================

    @SimpleFunction(description = "Génère la liste des catégories/sous-catégories depuis un JSON.")
    Public void BuildCategoryListFromJson(
            Final AndroidViewComponent listContainer,
            Final String categoriesJson) {

        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            Public void run() {
                Try {
                    Final JSONArray mainArray = new JSONArray(categoriesJson);

                    RunOnUi(new Runnable() {
                        @Override
                        Public void run() {
                            Try {
                                ViewGroup target = getRealLayout(listContainer);
                                If (target == null) return;

                                Target.removeAllViews();

                                RadioGroup group = new RadioGroup(activity);
                                Group.setOrientation(LinearLayout.VERTICAL);

                                ColorStateList radioColors = ColorStateList.valueOf(radioButtonColor);

                                For (int i = 0; i < mainArray.length(); i++) {
                                    JSONObject category = mainArray.getJSONObject(i);
                                    String categoryName = category.optString("title", "");
                                    JSONArray subCategories = category.optJSONArray("subcategories");

                                    TextView header = new TextView(activity);
                                    Header.setText(">  " + categoryName);
                                    Header.setTextColor(Color.parseColor("#E91A1A1B"));
                                    Header.setTextSize(18);
                                    Header.setTypeface(customTypeface, Typeface.BOLD);
                                    Header.setPadding(0, (int) dpToPx(16), 0, (int) dpToPx(8));

                                    Group.addView(header);

                                    If (subCategories != null) {
                                        For (int j = 0; j < subCategories.length(); j++) {
                                            JSONObject sub = subCategories.getJSONObject(j);
                                            Final String id = sub.optString("id", "");
                                            Final String title = sub.optString("title", "");

                                            RadioButton button = new RadioButton(activity);
                                            Button.setId(View.generateViewId());
                                            Button.setText(title);
                                            Button.setTextColor(Color.parseColor("#C01A1A1B"));
                                            Button.setTextSize(13);

                                            If (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                                Button.setButtonTintList(radioColors);
                                            }

                                            If (customTypeface != null) {
                                                Button.setTypeface(customTypeface);
                                            }

                                            Button.setPadding(
                                                    (int) dpToPx(8),
                                                    (int) dpToPx(12),
                                                    (int) dpToPx(8),
                                                    (int) dpToPx(12)
                                            );

                                            Button.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                Public void onClick(View v) {
                                                    OnCategorySelected(id, title);
                                                }
                                            });

                                            Group.addView(button);

                                            View divider = new View(activity);
                                            Divider.setLayoutParams(new LinearLayout.LayoutParams(
                                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                                    (int) dpToPx(1)
                                            ));
                                            Divider.setBackgroundColor(Color.parseColor("#F0F0F0"));

                                            Group.addView(divider);
                                        }
                                    }
                                }

                                Target.addView(group);

                            } catch (Exception e) {
                                E.printStackTrace();
                            }
                        }
                    });

                } catch (Exception e) {
                    E.printStackTrace();
                }
            }
        });
    }

    // =========================================================================
    // 4. EFFETS VISUELS
    // =========================================================================

    @SimpleFunction(description = "Applique un dégradé de couleur sur un composant.")
    Public void SetGradientBackground(
            Final AndroidViewComponent component,
            Final int startColor,
            Final int endColor,
            Final String orientation) {

        Activity.runOnUiThread(new Runnable() {
            @Override
            Public void run() {
                Try {
                    GradientDrawable.Orientation gradOrientation = GradientDrawable.Orientation.TOP_BOTTOM;

                    If ("LEFT_RIGHT".equalsIgnoreCase(orientation)) {
                        GradOrientation = GradientDrawable.Orientation.LEFT_RIGHT;
                    }

                    GradientDrawable gd = new GradientDrawable(
                            GradOrientation,
                            New int[]{startColor, endColor}
                    );
                    Gd.setCornerRadius(0f);
                    Component.getView().setBackground(gd);

                } catch (Exception e) {
                    E.printStackTrace();
                }
            }
        });
    }

    @SimpleFunction(description = "Applique un effet de flou (Glassmorphism) sur un composant.")
    Public void SetBlurEffect(final AndroidViewComponent component, final float radius) {
        Activity.runOnUiThread(new Runnable() {
            @Override
            Public void run() {
                Try {
                    View view = component.getView();

                    If (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Float blurRadius = Math.max(1f, Math.min(radius, 25f));
                        View.setRenderEffect(
                                RenderEffect.createBlurEffect(
                                        BlurRadius,
                                        BlurRadius,
                                        Shader.TileMode.CLAMP
                                )
                        );
                    } else {
                        View.setBackgroundColor(Color.argb(150, 255, 255, 255));
                    }
                } catch (Exception e) {
                    E.printStackTrace();
                }
            }
        });
    }

    // =========================================================================
    // 5. DIALOGUE TRANSPARENT, NOTIFICATION & GESTION SONORE
    // =========================================================================

    @SimpleFunction(description = "Affiche un composant sous forme de dialogue transparent.")
    Public void ShowAlphaDialog(
            Final AndroidViewComponent dialogContentLayout,
            Final boolean cancelable) {

        Activity.runOnUiThread(new Runnable() {
            @Override
            Public void run() {
                Try {
                    If (activeAlphaDialog != null && activeAlphaDialog.isShowing()) {
                        ActiveAlphaDialog.dismiss();
                    }

                    ActiveAlphaDialog = new Dialog(activity);
                    ActiveAlphaDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

                    View contentView = dialogContentLayout.getView();
                    If (contentView.getParent() != null) {
                        ((ViewGroup) contentView.getParent()).removeView(contentView);
                    }

                    ActiveAlphaDialog.setContentView(contentView);

                    If (activeAlphaDialog.getWindow() != null) {
                        ActiveAlphaDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                        ActiveAlphaDialog.getWindow().setLayout(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        );
                    }

                    ActiveAlphaDialog.setCancelable(cancelable);
                    ActiveAlphaDialog.show();

                } catch (Exception e) {
                    E.printStackTrace();
                }
            }
        });
    }

    @SimpleFunction(description = "Ferme le dialogue Alpha.")
    Public void DismissAlphaDialog() {
        Activity.runOnUiThread(new Runnable() {
            @Override
            Public void run() {
                If (activeAlphaDialog != null && activeAlphaDialog.isShowing()) {
                    ActiveAlphaDialog.dismiss();
                    ActiveAlphaDialog = null;
                }
            }
        });
    }

    @SimpleFunction(description = "Notification personnalisée temporaire.")
    Public void CustomNotifier(final AndroidViewComponent customLayout, final int durationMs) {
        Activity.runOnUiThread(new Runnable() {
            @Override
            Public void run() {
                ShowAlphaDialog(customLayout, true);
                New Handler().postDelayed(new Runnable() {
                    @Override
                    Public void run() {
                        DismissAlphaDialog();
                    }
                }, durationMs);
            }
        });
    }

    @SimpleFunction(description = "Joue un son personnalisé (ex: envoi de message, notification).")
    Public void PlayCustomSound(final String fileNameOrPath) {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            Public void run() {
                MediaPlayer mediaPlayer = null;
                Try {
                    MediaPlayer = new MediaPlayer();
                    MediaPlayer.setAudioAttributes(
                            New AudioAttributes.Builder()
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                                    .build()
                    );

                    If (fileNameOrPath.startsWith("/")) {
                        MediaPlayer.setDataSource(fileNameOrPath);
                    } else {
                        Android.content.res.AssetFileDescriptor afd = context.getAssets().openFd(fileNameOrPath);
                        MediaPlayer.setDataSource(
                                Afd.getFileDescriptor(),
                                Afd.getStartOffset(),
                                Afd.getLength()
                        );
                        Afd.close();
                    }

                    MediaPlayer.prepare();
                    MediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        Public void onCompletion(MediaPlayer mp) {
                            Mp.release();
                        }
                    });
                    MediaPlayer.start();

                } catch (Exception e) {
                    E.printStackTrace();
                    If (mediaPlayer != null) {
                        MediaPlayer.release();
                    }
                }
            }
        });
    }

    // =========================================================================
    // 6. GALERIE D'IMAGES & COMPRESSION
    // =========================================================================

    @SimpleFunction(description = "Ouvre la galerie d'images native.")
    Public void OpenPhotoPicker() {
        Activity.runOnUiThread(new Runnable() {
            @Override
            Public void run() {
                Try {
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    Intent.setType("image/*");
                    Activity.startActivityForResult(intent, PICK_IMAGE_REQUEST);
                } catch (Exception e) {
                    E.printStackTrace();
                }
            }
        });
    }

    @Override
    Public void resultReturned(int requestCode, int resultCode, Intent data) {
        If (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            If (selectedImageUri != null) {
                OnPhotoPicked(selectedImageUri.toString());
            }
        }
    }

    @SimpleFunction(description = "Compresse une image sans surcharger la mémoire.")
    Public String CompressImage(String imagePath, int quality, int maxWidth) {
        Try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            Options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imagePath, options);

            If (options.outWidth <= 0 || options.outHeight <= 0) {
                Return imagePath;
            }

            Int srcWidth = options.outWidth;
            Int inSampleSize = 1;

            If (srcWidth > maxWidth) {
                InSampleSize = Math.round((float) srcWidth / (float) maxWidth);
            }

            Options.inJustDecodeBounds = false;
            Options.inSampleSize = inSampleSize;

            Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);
            If (bitmap == null) return imagePath;

            File outputFile = new File(
                    Context.getCacheDir(),
                    "comp_" + System.currentTimeMillis() + ".jpg"
            );

            FileOutputStream out = null;
            Try {
                Out = new FileOutputStream(outputFile);
                Bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
                Out.flush();
            } finally {
                If (out != null) out.close();
                Bitmap.recycle();
            }

            Return outputFile.getAbsolutePath();

        } catch (Exception e) {
            E.printStackTrace();
            Return imagePath;
        }
    }

    // =========================================================================
    // 7. REQUÊTES SERVEUR
    // =========================================================================

    @SimpleFunction(description = "Envoie une requête HTTPS au serveur.")
    Public void CallServerRequest(
            Final String endpointUrl,
            Final String method,
            Final String headersJson,
            Final String bodyJson) {

        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            Public void run() {
                HttpURLConnection conn = null;
                Try {
                    URL url = new URL(endpointUrl);
                    Conn = (HttpURLConnection) url.openConnection();
                    Conn.setConnectTimeout(15000);
                    Conn.setReadTimeout(15000);
                    Conn.setRequestMethod("POST".equalsIgnoreCase(method) ? "POST" : "GET");

                    If (headersJson != null && !headersJson.isEmpty()) {
                        JSONObject headers = new JSONObject(headersJson);
                        Iterator<String> keys = headers.keys();
                        While (keys.hasNext()) {
                            String key = keys.next();
                            Conn.setRequestProperty(key, headers.getString(key));
                        }
                    }

                    If ("POST".equalsIgnoreCase(method)) {
                        Conn.setDoOutput(true);
                        Conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                        If (bodyJson != null) {
                            OutputStream os = conn.getOutputStream();
                            Os.write(bodyJson.getBytes("UTF-8"));
                            Os.flush();
                            Os.close();
                        }
                    }

                    Final int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 400)
                            ? Conn.getInputStream()
                            : conn.getErrorStream();

                    Final String responseContent = lireFlux(is);

                    Activity.runOnUiThread(new Runnable() {
                        @Override
                        Public void run() {
                            OnServerResponse(responseCode, responseContent);
                        }
                    });

                } catch (final Exception e) {
                    Activity.runOnUiThread(new Runnable() {
                        @Override
                        Public void run() {
                            OnServerResponse(500, e.getMessage());
                        }
                    });
                } finally {
                    If (conn != null) conn.disconnect();
                }
            }
        });
    }

    Private String lireFlux(InputStream is) throws IOException {
        If (is == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String ligne;
        While ((ligne = reader.readLine()) != null) {
            Sb.append(ligne);
        }
        Reader.close();
        Return sb.toString();
    }

    // =========================================================================
    // 8. ÉVÉNEMENTS KODULAR
    // =========================================================================

    @SimpleEvent(description = "Déclenché quand l'utilisateur touche une icône de la barre de navigation.")
    Public void OnSelected(String id) {
        EventDispatcher.dispatchEvent(this, "OnSelected", id);
    }

    @SimpleEvent(description = "Déclenché en cas de problème avec la barre de navigation.")
    Public void NavBarError(String message) {
        EventDispatcher.dispatchEvent(this, "NavBarError", message);
    }

    @SimpleEvent(description = "Déclenché lors du clic sur une carte produit.")
    Public void OnProductCardClick(String productUid) {
        EventDispatcher.dispatchEvent(this, "OnProductCardClick", productUid);
    }

    @SimpleEvent(description = "Déclenché lors du choix d'une catégorie. Renvoie l'ID et le Nom.")
    Public void OnCategorySelected(String categoryId, String categoryTitle) {
        EventDispatcher.dispatchEvent(this, "OnCategorySelected", categoryId, categoryTitle);
    }

    @SimpleEvent(description = "Déclenché lors du clic sur l'avatar du message.")
    Public void OnAvatarClick(boolean isMe) {
        EventDispatcher.dispatchEvent(this, "OnAvatarClick", isMe);
    }

    @SimpleEvent(description = "Déclenché après sélection d'une image.")
    Public void OnPhotoPicked(String imageUri) {
        EventDispatcher.dispatchEvent(this, "OnPhotoPicked", imageUri);
    }

    @SimpleEvent(description = "Déclenché après réponse du serveur.")
    Public void OnServerResponse(int responseCode, String responseContent) {
        EventDispatcher.dispatchEvent(this, "OnServerResponse", responseCode, responseContent);
    }
}

