package com.devkit.utils;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.*;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

@DesignerComponent(version = 19,
 description = "ManaplaceUtils - Design Seven, saisie, date/heure, image du billet et chat.",
 category = ComponentCategory.EXTENSION, nonVisible = true)
@SimpleObject(external = true)
@UsesPermissions(permissionNames = "android.permission.INTERNET,android.permission.READ_MEDIA_IMAGES,android.permission.READ_EXTERNAL_STORAGE")
public class ManaplaceUtils extends AndroidNonvisibleComponent implements ActivityResultListener {
  private final Context context;
  private final Activity activity;
  private final int requestCode;
  private Typeface manrope = Typeface.create("sans-serif-medium", Typeface.NORMAL);
  private final Map<String, EditText> inputs = new HashMap<>();
  private Uri ticketImageUri;

  private static final int FIELD = Color.rgb(26,26,27);
  private static final int STROKE = Color.rgb(158,158,158);
  private static final int FOCUS = Color.rgb(238,238,238);
  private static final int HINT = Color.rgb(158,158,158);

  public ManaplaceUtils(ComponentContainer container) {
    super(container.$form());
    context = container.$context();
    activity = (Activity) container.$context();
    requestCode = form.registerForActivityResult(this);
  }

  private int dp(float n) {
    return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,n,
      context.getResources().getDisplayMetrics());
  }
  private int pct(int base,float p) { return Math.max(1,Math.round(base*p/100f)); }
  private int screenHeight() { return context.getResources().getDisplayMetrics().heightPixels; }
  private int hostWidth(ViewGroup host) {
    int w=host.getWidth();
    return w>0?w:context.getResources().getDisplayMetrics().widthPixels;
  }
  private GradientDrawable shape(int color,int stroke,int strokeDp,int radiusDp) {
    GradientDrawable d=new GradientDrawable();
    d.setColor(color); d.setCornerRadius(dp(radiusDp)); d.setStroke(dp(strokeDp),stroke);
    return d;
  }
  private void add(ViewGroup host,View child,int w,int h) {
    if(host instanceof LinearLayout) {
      LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);
      p.gravity=Gravity.CENTER_HORIZONTAL; p.setMargins(0,dp(4),0,dp(4)); host.addView(child,p);
    } else if(host instanceof FrameLayout) {
      FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(w,h,Gravity.CENTER_HORIZONTAL);
      p.setMargins(0,dp(4),0,dp(4)); host.addView(child,p);
    } else host.addView(child,new ViewGroup.LayoutParams(w,h));
  }
  private EditText edit(String hint,boolean password) {
    EditText e=new EditText(context); e.setSingleLine(true); e.setBackgroundColor(Color.TRANSPARENT);
    e.setHint(hint==null?"":hint); e.setHintTextColor(HINT); e.setTextColor(Color.WHITE);
    e.setTextSize(TypedValue.COMPLEX_UNIT_SP,13); e.setTypeface(manrope);
    e.setPadding(dp(16),0,dp(16),0);
    if(password)e.setInputType(android.text.InputType.TYPE_CLASS_TEXT|
      android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
    return e;
  }
  private void strokeOnFocus(final View v,final GradientDrawable bg) {
    v.setOnFocusChangeListener((view,focus)->bg.setStroke(dp(focus?2:1),focus?FOCUS:STROKE));
  }

  @SimpleFunction(description="Charge fonts/Manrope-Medium.ttf depuis les assets de l'application.")
  public void LoadManropeMedium() {
    try {
      manrope=Typeface.createFromAsset(context.getAssets(),"fonts/Manrope-Medium.ttf");
      OnFontLoaded(true,"Manrope Medium chargée.");
    } catch(Exception e) {
      OnFontLoaded(false,"Police absente : ajoute fonts/Manrope-Medium.ttf aux assets. Police système utilisée.");
    }
  }
  @SimpleEvent public void OnFontLoaded(boolean success,String message) {
    EventDispatcher.dispatchEvent(this,"OnFontLoaded",success,message);
  }

  @SimpleFunction(description="Champ Seven : hauteur 9% de l'écran, largeur 93% du conteneur, texte 13sp.")
  public void BuildSevenInputCard(final AndroidViewComponent component,final String fieldId,
                                  final String hintText,final boolean isPassword) {
    if(component==null||component.getView()==null)return;
    final ViewGroup host=(ViewGroup)component.getView();
    host.post(()->{
      FrameLayout box=new FrameLayout(context);
      GradientDrawable bg=shape(FIELD,STROKE,1,15); box.setBackground(bg);
      EditText e=edit(hintText,isPassword);
      box.addView(e,new FrameLayout.LayoutParams(-1,-1));
      strokeOnFocus(e,bg); inputs.put(fieldId,e);
      add(host,box,pct(hostWidth(host),93),pct(screenHeight(),9));
    });
  }

  @SimpleFunction(description="Champ date Seven : hauteur 9%, largeur 55% du conteneur. Ouvre le calendrier.")
  public void BuildSevenDateInput(AndroidViewComponent component,String fieldId,String hint) {
    buildDateTime(component,fieldId,hint,true,55f);
  }
  @SimpleFunction(description="Champ heure Seven : hauteur 9%, largeur 36.6% du conteneur. Ouvre le sélecteur d'heure.")
  public void BuildSevenTimeInput(AndroidViewComponent component,String fieldId,String hint) {
    buildDateTime(component,fieldId,hint,false,36.6f);
  }
  private void buildDateTime(AndroidViewComponent component,String id,String hint,boolean date,float widthPct) {
    if(component==null||component.getView()==null)return;
    final ViewGroup host=(ViewGroup)component.getView();
    host.post(()->{
      FrameLayout box=new FrameLayout(context);
      GradientDrawable bg=shape(FIELD,STROKE,1,15); box.setBackground(bg);
      EditText e=edit(hint,false); e.setFocusable(false); e.setClickable(true);
      box.addView(e,new FrameLayout.LayoutParams(-1,-1)); inputs.put(id,e);
      e.setOnClickListener(v->{
        bg.setStroke(dp(2),FOCUS); Calendar c=Calendar.getInstance();
        if(date) new DatePickerDialog(activity,(picker,y,m,d)->{
          String value=String.format(java.util.Locale.US,"%02d/%02d/%04d",d,m+1,y);
          e.setText(value); bg.setStroke(dp(1),STROKE); OnSevenInputChanged(id,value);
        },c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();
        else new TimePickerDialog(activity,(picker,hour,minute)->{
          String value=String.format(java.util.Locale.US,"%02d:%02d",hour,minute);
          e.setText(value); bg.setStroke(dp(1),STROKE); OnSevenInputChanged(id,value);
        },c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),true).show();
      });
      add(host,box,pct(hostWidth(host),widthPct),pct(screenHeight(),9));
    });
  }
  @SimpleFunction(description="Retourne le texte saisi dans un champ Seven.")
  public String GetSevenInputValue(String fieldId) {
    EditText e=inputs.get(fieldId); return e==null||e.getText()==null?"":e.getText().toString().trim();
  }
  @SimpleEvent public void OnSevenInputChanged(String fieldId,String value) {
    EventDispatcher.dispatchEvent(this,"OnSevenInputChanged",fieldId,value);
  }

  @SimpleFunction(description="Crée la Card Image du billet Seven, hauteur 30%, largeur 93%, avec sélection d'image.")
  public void BuildSevenTicketImageCard(AndroidViewComponent component) {
    if(component==null||component.getView()==null)return;
    final ViewGroup host=(ViewGroup)component.getView();
    host.post(()->{
      LinearLayout card=new LinearLayout(context); card.setOrientation(LinearLayout.VERTICAL);
      card.setGravity(Gravity.CENTER); card.setPadding(dp(12),dp(12),dp(12),dp(12));
      card.setBackground(shape(FIELD,STROKE,1,18));
      TextView label=new TextView(context); label.setText("Ajouter l'image du billet");
      label.setTextColor(Color.WHITE); label.setTextSize(13); label.setTypeface(manrope);
      label.setGravity(Gravity.CENTER);
      card.addView(label,new LinearLayout.LayoutParams(-1,0,1));
      TextView choose=new TextView(context); choose.setText("Choisir une image  ·  PNG ou JPG");
      choose.setTextColor(HINT); choose.setTextSize(12); choose.setGravity(Gravity.CENTER);
      choose.setBackground(shape(Color.TRANSPARENT,STROKE,1,12));
      card.addView(choose,new LinearLayout.LayoutParams(-1,dp(58)));
      choose.setOnClickListener(v->{
        Intent i=new Intent(Intent.ACTION_GET_CONTENT); i.setType("image/*");
        i.addCategory(Intent.CATEGORY_OPENABLE); activity.startActivityForResult(i,requestCode);
      });
      add(host,card,pct(hostWidth(host),93),pct(screenHeight(),30));
    });
  }
  @SimpleFunction(description="Retourne l'URI de l'image du billet sélectionnée.")
  public String GetSevenTicketImageUri(){return ticketImageUri==null?"":ticketImageUri.toString();}
  @SimpleEvent public void OnSevenTicketImageSelected(String uri) {
    EventDispatcher.dispatchEvent(this,"OnSevenTicketImageSelected",uri);
  }

  @SimpleFunction(description="Bouton Google Play Seven : hauteur 9%, largeur 40%, fond noir, contour blanc 2dp.")
  public void BuildSevenGooglePlayButton(AndroidViewComponent component,String buttonText,String subDescription) {
    if(component==null||component.getView()==null)return;
    final ViewGroup host=(ViewGroup)component.getView();
    host.post(()->{
      LinearLayout button=new LinearLayout(context); button.setOrientation(LinearLayout.VERTICAL);
      button.setGravity(Gravity.CENTER); button.setPadding(dp(4),dp(2),dp(4),dp(2));
      button.setBackground(shape(Color.BLACK,Color.WHITE,2,15));
      TextView title=new TextView(context); title.setText(buttonText==null?"Google Play":buttonText);
      title.setTextColor(Color.WHITE); title.setTextSize(13); title.setTypeface(manrope);
      title.setGravity(Gravity.CENTER); button.addView(title);
      if(subDescription!=null&&!subDescription.trim().isEmpty()){
        TextView sub=new TextView(context); sub.setText(subDescription); sub.setTextColor(HINT);
        sub.setTextSize(13); sub.setTypeface(manrope); sub.setGravity(Gravity.CENTER); button.addView(sub);
      }
      button.setOnClickListener(v->OnGooglePlayPaymentClick());
      add(host,button,pct(hostWidth(host),40),pct(screenHeight(),9));
    });
  }
  @SimpleEvent(description="Clic sur le bouton Google Play. Cet événement ne traite pas lui-même le paiement.")
  public void OnGooglePlayPaymentClick(){EventDispatcher.dispatchEvent(this,"OnGooglePlayPaymentClick");}

  // Fonctions de chat conservées de la version fournie.
  @SimpleFunction(description="Active l'envoi du message avec la touche Envoyer du clavier.")
  public void EnableChatInputListener(AndroidViewComponent component) {
    if(component==null||component.getView()==null||!(component.getView() instanceof EditText))return;
    final EditText input=(EditText)component.getView();
    input.setOnEditorActionListener((v,actionId,event)->{
      boolean send=actionId==android.view.inputmethod.EditorInfo.IME_ACTION_SEND||
        actionId==android.view.inputmethod.EditorInfo.IME_ACTION_DONE;
      if(send){String message=input.getText()==null?"":input.getText().toString().trim();
        if(!message.isEmpty()){input.setText("");OnChatMessageSent(message);} return true;}
      return false;
    });
  }
  @SimpleEvent public void OnChatMessageSent(String message) {
    EventDispatcher.dispatchEvent(this,"OnChatMessageSent",message);
  }
  @SimpleEvent public void OnError(String message) {
    EventDispatcher.dispatchEvent(this,"OnError",message);
  }

  @Override public void resultReturned(int returnedRequestCode,int resultCode,Intent data) {
    if(returnedRequestCode!=requestCode||resultCode!=Activity.RESULT_OK||data==null)return;
    ticketImageUri=data.getData();
    if(ticketImageUri!=null)OnSevenTicketImageSelected(ticketImageUri.toString());
  }
}
