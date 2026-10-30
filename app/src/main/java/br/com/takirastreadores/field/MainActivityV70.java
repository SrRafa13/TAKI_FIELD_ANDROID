package br.com.takirastreadores.field;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.media.AudioManager;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class MainActivityV70 extends MainActivityV611 {
    static final String VERSION_70="7.0.0";

    @Override public void onCreate(Bundle b){super.onCreate(b);}

    @Override void screenHeader(String title,String sub,String active){super.screenHeader(title,sub,active);replace70(list);}
    @Override void welcomeScreen(){super.welcomeScreen();replace70(list);}
    void replace70(View v){
        if(v instanceof TextView){TextView t=(TextView)v;String s=String.valueOf(t.getText());s=s.replace("FIELD OPS • 6.1.1","FIELD OPS • 7.0").replace("FIELD OPERATIONS • 6.1.1","FIELD OPERATIONS • 7.0").replace("FIELD OPS • 6.1","FIELD OPS • 7.0").replace("FIELD OPERATIONS • 6.1","FIELD OPERATIONS • 7.0");t.setText(s);}
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)replace70(g.getChildAt(i));}
    }

    @Override Button button(String s,boolean primary,View.OnClickListener l){
        Button b=super.button(s,primary,null);b.setOnClickListener(v->{feedback(false);if(l!=null)l.onClick(v);});return b;
    }
    @Override TextView navChip(String text,boolean selected,View.OnClickListener l){
        TextView v=super.navChip(text,selected,null);v.setOnClickListener(x->{feedback(false);HorizontalScrollView sc=findScrollParent(v);if(sc!=null)prefs.edit().putInt("nav_scroll_x",sc.getScrollX()).apply();if(l!=null)l.onClick(x);});return v;
    }
    void feedback(boolean important){
        try{if(prefs.getBoolean("haptic_feedback_v70",true))getWindow().getDecorView().performHapticFeedback(important?HapticFeedbackConstants.LONG_PRESS:HapticFeedbackConstants.KEYBOARD_TAP);}catch(Exception ignored){}
        try{if(prefs.getBoolean("sound_feedback_v70",false)){AudioManager a=(AudioManager)getSystemService(AUDIO_SERVICE);if(a!=null)a.playSoundEffect(important?AudioManager.FX_KEYPRESS_RETURN:AudioManager.FX_KEY_CLICK,0.35f);}}catch(Exception ignored){}
    }

    @Override void centralScreen(){
        screenHeader("CENTRAL TÉCNICA","Operação de campo mais rápida, leve e organizada.","CENTRAL");
        File draft=new File(getFilesDir(),"installation_batch_v5.json");
        if(draft.exists()){LinearLayout resume=card();LinearLayout r=row();r.addView(icon("↻"),new LinearLayout.LayoutParams(dp(46),dp(46)));LinearLayout t=new LinearLayout(this);t.setOrientation(LinearLayout.VERTICAL);t.addView(txt("Lote salvo no aparelho",14,themeText(),true));t.addView(txt("Continue exatamente de onde parou.",12,themeMuted(),false));r.addView(t,new LinearLayout.LayoutParams(0,-2,1));r.addView(button("RETOMAR",true,v->openInstallations()),new LinearLayout.LayoutParams(dp(100),dp(44)));resume.addView(r);list.addView(resume,mb(11));}
        featureCard("▣","INSTALAÇÕES","Relatórios individuais ou em lote, agora com fotos totalmente opcionais.",this::openInstallations,40);
        featureCard("♟","CLIENTES","Base de clientes, endereços, favoritos e rota rápida.",this::clientsScreen,65);
        featureCard("⌖","GPS","Waze ou Google Maps, destinos recentes e compartilhados.",this::gpsScreen,90);
        featureCard("◫","DASHBOARD","Calendário real do aparelho, métricas e filtros.",this::dashboardScreen,115);
        featureCard("▦","HISTÓRICO","Serviços e PDFs arquivados por placa.",this::serviceHistoryScreen,140);
        featureCard("⚡","DIAGNÓSTICO GUIADO","Comunicação, GPS, ignição, bloqueio e RS232.",this::diagnosticsScreen,165);
        ArrayList<ServiceStore.Entry> recent=ServiceStore.list(this);if(!recent.isEmpty()){LinearLayout c=card();c.addView(txt("ÚLTIMOS SERVIÇOS",13,themeMuted(),true),mb(7));for(int i=0;i<Math.min(3,recent.size());i++){ServiceStore.Entry e=recent.get(i);c.addView(txt(e.plate+"  •  "+e.service+"  •  "+e.technician,12,themeText(),i==0),mb(5));}list.addView(c,mb(10));}
    }

    void clientsScreen(){
        screenHeader("CLIENTES","Seus destinos salvos viraram uma central própria para o trabalho de campo.","GPS");
        ArrayList<Destination> saved=loadDestinations();Set<String> favs=new HashSet<>(prefs.getStringSet("gps_favorites_v61",Collections.emptySet()));Collections.sort(saved,(a,b)->Boolean.compare(favs.contains(favKey(b)),favs.contains(favKey(a))));
        LinearLayout add=card();add.addView(txt("NOVO CLIENTE",15,themeText(),true),mb(8));EditText name=input("Nome do cliente / base");EditText address=input("Endereço, coordenadas ou localização");add.addView(name,mb(8));add.addView(address,mb(8));add.addView(button("SALVAR CLIENTE",true,v->{String val=extractDestination(address.getText().toString());if(val.isEmpty()){address.setError("Informe o endereço ou localização");return;}String n=name.getText().toString().trim();if(n.isEmpty())n="CLIENTE "+(loadDestinations().size()+1);saveClientDirect(n,val);clientsScreen();}),new LinearLayout.LayoutParams(-1,dp(48)));list.addView(add,mb(12));
        list.addView(txt("CLIENTES • "+saved.size(),12,themeMuted(),true),mb(8));
        if(saved.isEmpty()){LinearLayout e=card();e.addView(txt("Nenhum cliente salvo ainda.",14,themeText(),true),mb(4));e.addView(txt("Cadastre acima ou salve um destino pela área de GPS.",12,themeMuted(),false));list.addView(e);return;}
        for(Destination d:saved){boolean fav=favs.contains(favKey(d));LinearLayout c=card();LinearLayout r=row();TextView star=txt(fav?"★":"☆",23,fav?theme().accent:themeMuted(),true);star.setGravity(Gravity.CENTER);star.setOnClickListener(v->{feedback(false);toggleFavorite(d);clientsScreen();});r.addView(star,new LinearLayout.LayoutParams(dp(42),dp(50)));LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.addView(txt(d.name,15,themeText(),true));tx.addView(txt(d.value,12,themeMuted(),false));r.addView(tx,new LinearLayout.LayoutParams(0,-2,1));c.addView(r,mb(8));LinearLayout actions=row();actions.addView(button("IR →",true,v->routeTo(d.value)),new LinearLayout.LayoutParams(0,dp(44),1));gap(actions);actions.addView(button("EXCLUIR",false,v->confirmDeleteClient(d)),new LinearLayout.LayoutParams(0,dp(44),1));c.addView(actions);list.addView(c,mb(9));}
    }

    void saveClientDirect(String name,String value){
        try{JSONArray a=new JSONArray(prefs.getString("gps_destinations_v6","[]"));JSONObject o=new JSONObject();o.put("name",name);o.put("value",value);a.put(o);prefs.edit().putString("gps_destinations_v6",a.toString()).apply();Toast.makeText(this,"Cliente salvo",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"Não foi possível salvar",Toast.LENGTH_SHORT).show();}
    }

    void removeClientDirect(Destination d){
        try{
            JSONArray source=new JSONArray(prefs.getString("gps_destinations_v6","[]"));
            JSONArray kept=new JSONArray();
            for(int i=0;i<source.length();i++){
                JSONObject o=source.optJSONObject(i);
                if(o==null)continue;
                boolean same=Objects.equals(o.optString("name",""),d.name)&&Objects.equals(o.optString("value",""),d.value);
                if(!same)kept.put(o);
            }
            Set<String> favs=new HashSet<>(prefs.getStringSet("gps_favorites_v61",Collections.emptySet()));
            favs.remove(favKey(d));
            prefs.edit().putString("gps_destinations_v6",kept.toString()).putStringSet("gps_favorites_v61",favs).apply();
            Toast.makeText(this,"Cliente excluído",Toast.LENGTH_SHORT).show();
        }catch(Exception e){Toast.makeText(this,"Não foi possível excluir",Toast.LENGTH_SHORT).show();}
    }
    void confirmDeleteClient(Destination d){new AlertDialog.Builder(this).setTitle("Excluir cliente?").setMessage(d.name+"\n"+d.value).setNegativeButton("Cancelar",null).setPositiveButton("EXCLUIR",(x,w)->{removeClientDirect(d);clientsScreen();}).show();}

    @Override void settingsScreen(){
        super.settingsScreen();
        LinearLayout f=card();f.addView(txt("TOQUE E SOM",15,themeText(),true),mb(4));f.addView(txt("Feedback discreto para confirmar ações sem deixar o app barulhento.",12,themeMuted(),false),mb(10));boolean h=prefs.getBoolean("haptic_feedback_v70",true),s=prefs.getBoolean("sound_feedback_v70",false);f.addView(button("VIBRAÇÃO • "+(h?"LIGADA":"DESLIGADA"),h,v->{prefs.edit().putBoolean("haptic_feedback_v70",!prefs.getBoolean("haptic_feedback_v70",true)).apply();settingsScreen();}),new LinearLayout.LayoutParams(-1,dp(46)));f.addView(button("SONS DE TOQUE • "+(s?"LIGADOS":"DESLIGADOS"),s,v->{prefs.edit().putBoolean("sound_feedback_v70",!prefs.getBoolean("sound_feedback_v70",false)).apply();settingsScreen();}),mt(8));f.addView(txt("Os sons usam efeitos leves do próprio Android. Em temas sazonais, o visual continua temático sem sons longos ou repetitivos.",11,themeMuted(),false),mt(8));list.addView(f,mb(12));
    }

    @Override void openInstallations(){startActivity(new Intent(this,InstallationActivityV70.class));}

    @Override void checkUpdate(){Toast.makeText(this,"Consultando versão...",Toast.LENGTH_SHORT).show();new Thread(()->{try{URL u=new URL("https://raw.githubusercontent.com/SrRafa13/TAKI_FIELD_ANDROID/main/version.txt");BufferedReader br=new BufferedReader(new InputStreamReader(u.openStream()));String latest=br.readLine();br.close();runOnUiThread(()->new AlertDialog.Builder(this).setTitle("TAKI FIELD • Atualização").setMessage("Versão instalada: "+VERSION_70+"\nVersão disponível: "+latest+(VERSION_70.equals(latest)?"\n\nSeu app está atualizado.":"\n\nHá uma versão diferente disponível.")).setNegativeButton("Fechar",null).setPositiveButton("ABRIR VERSÕES",(d,w)->openUrl("https://github.com/SrRafa13/TAKI_FIELD_ANDROID/actions")).show());}catch(Exception e){runOnUiThread(()->Toast.makeText(this,"Não foi possível consultar atualização",Toast.LENGTH_LONG).show());}}).start();}
}
