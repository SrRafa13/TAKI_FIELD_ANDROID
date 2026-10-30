package br.com.takirastreadores.field;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.core.content.FileProvider;
import org.json.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivityV61 extends MainActivityV60 {
    static final String VERSION_61 = "6.1.0";
    static final int REQ_BACKUP_EXPORT = 6101;
    static final int REQ_BACKUP_IMPORT = 6102;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        boolean removed = cleanupStaleDraft();
        if (removed && "CENTRAL".equals(currentSection) && !prefs.getString("technician","").isEmpty()) centralScreen();
    }

    boolean cleanupStaleDraft() {
        File f = new File(getFilesDir(), "installation_batch_v5.json");
        if (!f.exists()) return false;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try(FileInputStream in=new FileInputStream(f)){byte[] b=new byte[4096];int n;while((n=in.read(b))>0)out.write(b,0,n);}
            JSONObject root=new JSONObject(new String(out.toByteArray(),"UTF-8"));
            JSONArray records=root.optJSONArray("records"); JSONObject current=root.optJSONObject("current");
            String plate=current==null?"":current.optString("plate","").trim();
            boolean empty=(records==null||records.length()==0)&&plate.isEmpty();
            if(empty)return f.delete();
        }catch(Exception ignored){}
        return false;
    }

    ThemeEngine.Theme theme(){ return prefs==null ? ThemeEngine.byKey(ThemeEngine.DARK) : ThemeEngine.current(prefs); }
    @Override boolean lightMode(){return theme().light;}
    @Override int themeBg(){return theme().bg;}
    @Override int themeSurface(){return theme().surface;}
    @Override int themeSurface2(){return theme().surface2;}
    @Override int themeText(){return theme().text;}
    @Override int themeMuted(){return theme().muted;}
    @Override int themeStroke(){return theme().stroke;}

    @Override TextView txt(String s,int size,int color,boolean bold){
        int mapped=color;
        if(color==WHITE)mapped=theme().text; else if(color==MUTED)mapped=theme().muted; else if(color==RED)mapped=theme().accent;
        TextView v=new TextView(this);v.setText(s);v.setTextColor(mapped);v.setTextSize(size);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;
    }

    @Override LinearLayout card(){
        LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setPadding(dp(16),dp(16),dp(16),dp(16));
        GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{theme().surface,theme().surface2});g.setCornerRadius(dp(18));g.setStroke(dp(1),theme().stroke);x.setBackground(g);x.setElevation(dp(2));return x;
    }

    @Override Button button(String s,boolean primary,View.OnClickListener l){
        Button b=new Button(this);b.setText(s);b.setTextColor(primary?Color.WHITE:theme().text);b.setTextSize(12);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setAllCaps(false);
        b.setBackground(round(primary?theme().accent:theme().surface2,14,primary?theme().accent2:theme().stroke,1));b.setOnClickListener(l);b.setStateListAnimator(null);return b;
    }

    @Override TextView icon(String s){TextView v=txt(s,20,theme().accent,true);v.setGravity(Gravity.CENTER);v.setBackground(round(theme().surface2,14,theme().stroke,1));return v;}

    @Override TextView navChip(String text,boolean selected,View.OnClickListener l){
        TextView v=txt(text,11,selected?Color.WHITE:theme().text,true);v.setGravity(Gravity.CENTER);v.setPadding(dp(14),0,dp(14),0);
        v.setBackground(round(selected?theme().accent:theme().surface2,99,selected?theme().accent:theme().stroke,1));
        v.setOnClickListener(x->{HorizontalScrollView sc=findScrollParent(v);if(sc!=null)prefs.edit().putInt("nav_scroll_x",sc.getScrollX()).apply();if(l!=null)l.onClick(x);});
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(40));p.rightMargin=dp(7);v.setLayoutParams(p);return v;
    }

    HorizontalScrollView findScrollParent(View v){ViewParent p=v.getParent();while(p!=null){if(p instanceof HorizontalScrollView)return (HorizontalScrollView)p;p=p.getParent();}return null;}

    @Override HorizontalScrollView navBar(String active){
        HorizontalScrollView s=super.navBar(active);int saved=prefs==null?0:prefs.getInt("nav_scroll_x",0);s.post(()->s.scrollTo(saved,0));return s;
    }

    @Override void screenHeader(String title,String sub,String active){
        currentSection=active==null?"":active;list.removeAllViews();applyBackground();
        LinearLayout top=row();
        LinearLayout brand=new LinearLayout(this);brand.setOrientation(LinearLayout.VERTICAL);brand.addView(txt("TAKI FIELD",21,themeText(),true));brand.addView(txt("FIELD OPS • 6.1",9,theme().accent,true));top.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout who=new LinearLayout(this);who.setOrientation(LinearLayout.VERTICAL);who.setGravity(Gravity.END);TextView tech=txt("●  "+prefs.getString("technician","-"),11,themeText(),true);tech.setGravity(Gravity.END);who.addView(tech);TextView swap=txt("trocar técnico",10,themeMuted(),false);swap.setGravity(Gravity.END);swap.setOnClickListener(v->{prefs.edit().remove("technician").apply();welcomeScreen();});who.addView(swap);top.addView(who);list.addView(top,mb(12));
        list.addView(navBar(active),mb(14));
        addPreviewBannerIfNeeded();
        list.addView(txt(title,24,themeText(),true),mb(4));if(sub!=null&&!sub.isEmpty())list.addView(txt(sub,13,themeMuted(),false),mb(10));
        String decor=ThemeEngine.decor(ThemeEngine.effectiveKey(prefs));
        if(!decor.isEmpty()){TextView d=txt(decor,14,theme().accent,true);d.setGravity(Gravity.CENTER);d.setAlpha(.65f);list.addView(d,mb(12));d.animate().alpha(1f).setDuration(650).withEndAction(()->d.animate().alpha(.72f).setDuration(650).start()).start();}
    }

    void addPreviewBannerIfNeeded(){
        String preview=prefs.getString("theme_preview","");if(preview.isEmpty())return;
        ThemeEngine.Theme t=ThemeEngine.byKey(preview);LinearLayout c=card();c.addView(txt("MODO DE DEMONSTRAÇÃO • "+t.emoji+" "+t.name,12,theme().accent,true),mb(8));
        LinearLayout r=row();r.addView(button("APLICAR TEMA",true,v->applyPreview()),new LinearLayout.LayoutParams(0,dp(44),1));gap(r);r.addView(button("VOLTAR",false,v->{prefs.edit().remove("theme_preview").apply();themesScreen();}),new LinearLayout.LayoutParams(0,dp(44),1));c.addView(r);list.addView(c,mb(12));
    }

    void applyPreview(){
        String key=prefs.getString("theme_preview",ThemeEngine.DARK);ThemeEngine.Theme t=ThemeEngine.byKey(key);
        prefs.edit().putString("theme_selected",key).putString("theme_mode",t.light?"light":"dark").remove("theme_preview").apply();
        Toast.makeText(this,"Tema aplicado: "+t.name,Toast.LENGTH_SHORT).show();recreate();
    }

    @Override void welcomeScreen(){
        currentSection="WELCOME";list.removeAllViews();applyBackground();Space top=new Space(this);list.addView(top,new LinearLayout.LayoutParams(1,dp(42)));
        TextView mark=txt(theme().emoji,25,theme().accent,true);mark.setGravity(Gravity.CENTER);list.addView(mark,mb(8));
        TextView brand=txt("TAKI FIELD",30,themeText(),true);brand.setGravity(Gravity.CENTER);list.addView(brand,mb(4));TextView version=txt("FIELD OPERATIONS • 6.1",11,theme().accent,true);version.setGravity(Gravity.CENTER);list.addView(version,mb(22));
        String decor=ThemeEngine.decor(ThemeEngine.effectiveKey(prefs));if(!decor.isEmpty()){TextView d=txt(decor,15,theme().accent,true);d.setGravity(Gravity.CENTER);list.addView(d,mb(15));}
        LinearLayout c=card();TextView hi=txt("BEM-VINDO",22,themeText(),true);hi.setGravity(Gravity.CENTER);c.addView(hi,mb(5));TextView sub=txt("Selecione quem está usando o aplicativo hoje.",13,themeMuted(),false);sub.setGravity(Gravity.CENTER);c.addView(sub,mb(18));c.addView(techButton("EDUARDO"),new LinearLayout.LayoutParams(-1,dp(56)));Space g=new Space(this);c.addView(g,new LinearLayout.LayoutParams(1,dp(10)));c.addView(techButton("RAFAEL"),new LinearLayout.LayoutParams(-1,dp(56)));list.addView(c,mb(18));TextView local=txt("Perfil salvo somente neste aparelho • funciona offline",11,themeMuted(),false);local.setGravity(Gravity.CENTER);list.addView(local);animateIn(c,80);
    }

    @Override void openInstallations(){startActivity(new Intent(this,InstallationActivityV61.class));}

    @Override void setThemeMode(boolean light){String key=light?ThemeEngine.LIGHT:ThemeEngine.DARK;prefs.edit().putString("theme_mode",key).putString("theme_selected",key).remove("theme_preview").apply();recreate();}

    @Override void settingsScreen(){
        super.settingsScreen();
        LinearLayout seasonal=card();seasonal.addView(txt("PERSONALIZAÇÃO 6.1",15,themeText(),true),mb(4));seasonal.addView(txt("Tema atual: "+theme().emoji+" "+theme().name,12,themeMuted(),false),mb(5));seasonal.addView(txt(ThemeEngine.nextPublicSeasonalLabel(),11,themeMuted(),false),mb(11));
        boolean auto=prefs.getBoolean("seasonal_auto",true);seasonal.addView(button("TEMAS SAZONAIS • "+(auto?"LIGADO":"DESLIGADO"),auto,v->{prefs.edit().putBoolean("seasonal_auto",!prefs.getBoolean("seasonal_auto",true)).apply();settingsScreen();}),new LinearLayout.LayoutParams(-1,dp(48)));seasonal.addView(button("VISUALIZAR TEMAS",false,v->themesScreen()),mt(8));list.addView(seasonal,mb(11));
        LinearLayout backup=card();backup.addView(txt("BACKUP E DADOS",15,themeText(),true),mb(4));backup.addView(txt("Leve histórico, preferências, clientes e PDFs arquivados para outro aparelho sem servidor.",12,themeMuted(),false),mb(11));LinearLayout br=row();br.addView(button("EXPORTAR BACKUP",true,v->startBackupExport()),new LinearLayout.LayoutParams(0,dp(48),1));gap(br);br.addView(button("RESTAURAR",false,v->confirmRestore()),new LinearLayout.LayoutParams(0,dp(48),1));backup.addView(br);list.addView(backup,mb(12));
    }

    void themesScreen(){
        screenHeader("VISUALIZAR TEMAS","Toque em qualquer tema para navegar pelo app em modo de demonstração.","AJUSTES");
        for(ThemeEngine.Theme t:ThemeEngine.all()){
            LinearLayout c=card();c.setClickable(true);c.setOnClickListener(v->{prefs.edit().putString("theme_preview",t.key).apply();centralScreen();});LinearLayout r=row();TextView em=txt(t.emoji,27,t.accent,true);em.setGravity(Gravity.CENTER);r.addView(em,new LinearLayout.LayoutParams(dp(48),dp(48)));LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.addView(txt(t.name,16,themeText(),true));tx.addView(txt("Toque para visualizar no aplicativo",11,themeMuted(),false));r.addView(tx,new LinearLayout.LayoutParams(0,-2,1));TextView go=txt("›",26,themeMuted(),false);go.setGravity(Gravity.CENTER);r.addView(go,new LinearLayout.LayoutParams(dp(30),dp(48)));c.addView(r,mb(10));
            LinearLayout colors=row();int[] cs={t.bg,t.surface,t.surface2,t.accent,t.accent2};for(int color:cs){TextView sw=new TextView(this);sw.setBackground(round(color,99,color,0));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(12),1);p.rightMargin=dp(5);colors.addView(sw,p);}c.addView(colors);list.addView(c,mb(9));
        }
    }

    @Override void routeTo(String raw){String d=extractDestination(raw);if(!d.isEmpty())addRecentDestination(d);super.routeTo(raw);}

    @Override void gpsScreen(){
        screenHeader("GPS","Escolha o navegador, salve clientes e inicie a rota em um toque.","GPS");
        LinearLayout provider=card();provider.addView(label("NAVEGADOR PADRÃO"),mb(8));LinearLayout pr=row();String savedProvider=prefs.getString("gps_provider","Waze");Button waze=navChoice("WAZE","Waze".equals(savedProvider));Button google=navChoice("GOOGLE MAPS","Google".equals(savedProvider));pr.addView(waze,new LinearLayout.LayoutParams(0,dp(48),1));gap(pr);pr.addView(google,new LinearLayout.LayoutParams(0,dp(48),1));provider.addView(pr);list.addView(provider,mb(11));
        LinearLayout dest=card();dest.addView(txt("NOVO DESTINO",15,themeText(),true),mb(4));dest.addView(txt("Digite endereço, coordenadas ou compartilhe uma localização com o TAKI FIELD.",12,themeMuted(),false),mb(10));EditText destination=input("Rua, número, cidade ou coordenadas");String pending=prefs.getString("pending_shared_location","");if(!pending.isEmpty()){destination.setText(extractDestination(pending));prefs.edit().remove("pending_shared_location").apply();}dest.addView(destination,mb(9));LinearLayout dr=row();dr.addView(button("INICIAR ROTA",true,v->routeTo(destination.getText().toString())),new LinearLayout.LayoutParams(0,dp(50),1));gap(dr);dr.addView(button("SALVAR",false,v->promptSaveDestination(destination.getText().toString())),new LinearLayout.LayoutParams(0,dp(50),1));dest.addView(dr,mb(8));dest.addView(button("PESQUISAR DESTINO",false,v->searchDestination(destination.getText().toString())),new LinearLayout.LayoutParams(-1,dp(46)));list.addView(dest,mb(12));

        ArrayList<String> recent=loadRecentDestinations();if(!recent.isEmpty()){LinearLayout rc=card();rc.addView(txt("DESTINOS RECENTES",13,themeMuted(),true),mb(8));for(String d:recent){LinearLayout rr=row();TextView tv=txt("◷  "+shorten(d,45),12,themeText(),false);rr.addView(tv,new LinearLayout.LayoutParams(0,dp(42),1));rr.addView(button("IR",false,v->routeTo(d)),new LinearLayout.LayoutParams(dp(60),dp(42)));gap(rr);rr.addView(button("SALVAR",false,v->promptSaveDestination(d)),new LinearLayout.LayoutParams(dp(76),dp(42)));rc.addView(rr,mb(6));}list.addView(rc,mb(12));}

        ArrayList<Destination> saved=loadDestinations();Set<String> favs=new HashSet<>(prefs.getStringSet("gps_favorites_v61",Collections.emptySet()));Collections.sort(saved,(a,b)->Boolean.compare(favs.contains(favKey(b)),favs.contains(favKey(a))));list.addView(txt("CLIENTES SALVOS  •  "+saved.size(),12,themeMuted(),true),mb(8));
        if(saved.isEmpty()){LinearLayout e=card();e.addView(txt("Nenhum cliente salvo ainda.",14,themeText(),true),mb(4));e.addView(txt("Digite o endereço acima e toque em SALVAR.",12,themeMuted(),false));list.addView(e,mb(10));}
        else for(Destination d:saved){boolean fav=favs.contains(favKey(d));LinearLayout c=card();LinearLayout rr=row();TextView star=txt(fav?"★":"☆",23,fav?theme().accent:themeMuted(),true);star.setGravity(Gravity.CENTER);star.setOnClickListener(v->{toggleFavorite(d);gpsScreen();});rr.addView(star,new LinearLayout.LayoutParams(dp(40),dp(48)));LinearLayout tt=new LinearLayout(this);tt.setOrientation(LinearLayout.VERTICAL);tt.addView(txt(d.name,15,themeText(),true));tt.addView(txt(d.value,12,themeMuted(),false));rr.addView(tt,new LinearLayout.LayoutParams(0,-2,1));rr.addView(button("IR →",true,v->routeTo(d.value)),new LinearLayout.LayoutParams(dp(78),dp(44)));c.addView(rr,mb(6));TextView del=txt("remover destino",11,themeMuted(),false);del.setGravity(Gravity.END);del.setOnClickListener(v->{removeFavorite(d);deleteDestination(d);gpsScreen();});c.addView(del);list.addView(c,mb(9));}
        waze.setOnClickListener(v->{prefs.edit().putString("gps_provider","Waze").apply();gpsScreen();});google.setOnClickListener(v->{prefs.edit().putString("gps_provider","Google").apply();gpsScreen();});
    }

    void searchDestination(String raw){
        String d=extractDestination(raw);if(d.isEmpty()){Toast.makeText(this,"Informe um destino",Toast.LENGTH_SHORT).show();return;}addRecentDestination(d);String provider=prefs.getString("gps_provider","Waze");
        try{if(isUrl(d)){openPreferred(d,"Google".equals(provider)?"com.google.android.apps.maps":"com.waze");return;}String q=URLEncoder.encode(d,"UTF-8");if("Google".equals(provider))openPreferred("https://www.google.com/maps/search/?api=1&query="+q,"com.google.android.apps.maps");else openPreferred("https://waze.com/ul?q="+q,"com.waze");}catch(Exception e){Toast.makeText(this,"Não foi possível pesquisar o destino",Toast.LENGTH_SHORT).show();}
    }

    String favKey(Destination d){return d.name+"\n"+d.value;}
    void toggleFavorite(Destination d){Set<String>s=new HashSet<>(prefs.getStringSet("gps_favorites_v61",Collections.emptySet()));String k=favKey(d);if(s.contains(k))s.remove(k);else s.add(k);prefs.edit().putStringSet("gps_favorites_v61",s).apply();}
    void removeFavorite(Destination d){Set<String>s=new HashSet<>(prefs.getStringSet("gps_favorites_v61",Collections.emptySet()));s.remove(favKey(d));prefs.edit().putStringSet("gps_favorites_v61",s).apply();}
    void addRecentDestination(String d){ArrayList<String>a=loadRecentDestinations();a.remove(d);a.add(0,d);while(a.size()>5)a.remove(a.size()-1);JSONArray j=new JSONArray();for(String x:a)j.put(x);prefs.edit().putString("gps_recents_v61",j.toString()).apply();}
    ArrayList<String> loadRecentDestinations(){ArrayList<String>a=new ArrayList<>();try{JSONArray j=new JSONArray(prefs.getString("gps_recents_v61","[]"));for(int i=0;i<j.length();i++){String x=j.optString(i,"");if(!x.isEmpty())a.add(x);}}catch(Exception ignored){}return a;}
    String shorten(String s,int n){return s.length()<=n?s:s.substring(0,n-1)+"…";}

    @Override void serviceHistoryScreen(){
        screenHeader("HISTÓRICO DE SERVIÇOS","Pesquise placas e reabra relatórios salvos no aparelho.","HISTÓRICO");ArrayList<ServiceStore.Entry> all=ServiceStore.list(this);
        LinearLayout f=card();EditText query=input("Buscar placa, módulo, técnico ou serviço");f.addView(query,mb(8));Spinner service=spinner(new String[]{"Todos os serviços","Instalação","Troca","Retirada","Manutenção"});f.addView(service,mb(8));Spinner tech=spinner(dashboardTechFilters(all));f.addView(tech);list.addView(f,mb(10));LinearLayout holder=new LinearLayout(this);holder.setOrientation(LinearLayout.VERTICAL);list.addView(holder);
        Runnable render=()->renderHistory61(holder,all,query.getText().toString(),service.getSelectedItem().toString(),tech.getSelectedItem().toString());query.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){render.run();}public void afterTextChanged(Editable e){}});AdapterView.OnItemSelectedListener sl=new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?>p,View v,int pos,long id){render.run();}};service.setOnItemSelectedListener(sl);tech.setOnItemSelectedListener(sl);render.run();
    }

    void renderHistory61(LinearLayout holder,ArrayList<ServiceStore.Entry> all,String q,String service,String tech){
        holder.removeAllViews();String needle=q.trim().toLowerCase(Locale.ROOT);int count=0;
        for(ServiceStore.Entry e:all){if(!"Todos os serviços".equals(service)&&!service.equals(e.service))continue;if(!"Todos os técnicos".equals(tech)&&!tech.equals(e.technician))continue;String hay=(e.plate+" "+e.module+" "+e.technician+" "+e.service+" "+e.brand).toLowerCase(Locale.ROOT);if(!needle.isEmpty()&&!hay.contains(needle))continue;count++;LinearLayout c=card();c.setOnClickListener(v->showServiceDialog(e));c.setClickable(true);LinearLayout top=row();LinearLayout t=new LinearLayout(this);t.setOrientation(LinearLayout.VERTICAL);t.addView(txt(e.plate,17,themeText(),true));t.addView(txt(e.service+" • "+e.finishedAt,12,themeMuted(),false));t.addView(txt("Téc. "+e.technician+(e.module.isEmpty()?"":" • "+e.brand+" "+e.module),11,themeMuted(),false));top.addView(t,new LinearLayout.LayoutParams(0,-2,1));if(!e.reportFile.isEmpty())top.addView(button("VER PDF",true,v->openArchivedReport(e)),new LinearLayout.LayoutParams(dp(82),dp(44)));c.addView(top,mb(5));String rt="batch".equals(e.reportType)?"📚 Relatório em lote • "+Math.max(1,e.reportCount)+" veículos":"📄 Relatório individual";c.addView(txt(e.reportFile.isEmpty()?"Relatório antigo sem PDF arquivado":rt,11,e.reportFile.isEmpty()?themeMuted():theme().accent,false));holder.addView(c,mb(9));}
        if(count==0){LinearLayout e=card();e.addView(txt("Nenhum serviço encontrado.",14,themeText(),true));holder.addView(e);}
    }

    void showServiceDialog(ServiceStore.Entry e){
        String msg=e.service+"\n"+e.finishedAt+"\nTécnico: "+e.technician+(e.brand.isEmpty()?"":"\nFabricante: "+e.brand)+(e.module.isEmpty()?"":"\nMódulo: "+e.module)+(e.batchId.isEmpty()?"":"\nLote: "+e.batchId);AlertDialog.Builder b=new AlertDialog.Builder(this).setTitle(e.plate).setMessage(msg).setNegativeButton("Fechar",null);if(!e.reportFile.isEmpty())b.setPositiveButton("VER RELATÓRIO",(d,w)->openArchivedReport(e));b.show();
    }

    void openArchivedReport(ServiceStore.Entry e){
        try{Uri u;if(e.reportFile.startsWith("uri:"))u=Uri.parse(e.reportFile.substring(4));else{File f=new File(new File(getFilesDir(),"reports"),e.reportFile);if(!f.exists()){Toast.makeText(this,"PDF não encontrado no aparelho",Toast.LENGTH_LONG).show();return;}u=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",f);}Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(u,"application/pdf");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(i);}catch(Exception x){Toast.makeText(this,"Não foi possível abrir o PDF",Toast.LENGTH_LONG).show();}
    }

    @Override void dashboardScreen(){
        screenHeader("DASHBOARD","Resumo interativo dos serviços salvos neste aparelho.","DASHBOARD");ArrayList<ServiceStore.Entry> entries=ServiceStore.list(this);LinearLayout f=card();f.addView(label("PERÍODO"));Spinner month=spinner(monthOptions(entries));f.addView(month,mb(8));f.addView(label("TIPO DE SERVIÇO"));Spinner service=spinner(new String[]{"Todos","Instalação","Troca","Retirada","Manutenção"});f.addView(service,mb(8));f.addView(label("TÉCNICO"));Spinner tech=spinner(dashboardTechFilters(entries));f.addView(tech);list.addView(f,mb(11));LinearLayout holder=new LinearLayout(this);holder.setOrientation(LinearLayout.VERTICAL);list.addView(holder);Runnable render=()->renderDashboard61(holder,entries,month.getSelectedItem().toString(),service.getSelectedItem().toString(),tech.getSelectedItem().toString());AdapterView.OnItemSelectedListener l=new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?>p){}public void onItemSelected(AdapterView<?>p,View v,int pos,long id){render.run();}};month.setOnItemSelectedListener(l);service.setOnItemSelectedListener(l);tech.setOnItemSelectedListener(l);render.run();
    }

    void renderDashboard61(LinearLayout holder,ArrayList<ServiceStore.Entry> all,String month,String service,String tech){
        holder.removeAllViews();ArrayList<ServiceStore.Entry> filtered=new ArrayList<>();SimpleDateFormat mf=new SimpleDateFormat("MM/yyyy",Locale.getDefault());for(ServiceStore.Entry e:all){if(e.epoch<=0||!month.equals(mf.format(new Date(e.epoch))))continue;if(!"Todos".equals(service)&&!service.equals(e.service))continue;if(!"Todos os técnicos".equals(tech)&&!tech.equals(e.technician))continue;filtered.add(e);}ArrayList<ServiceStore.Entry> installations=new ArrayList<>();LinkedHashSet<String> plates=new LinkedHashSet<>();for(ServiceStore.Entry e:filtered){if("Instalação".equals(e.service))installations.add(e);plates.add(e.plate);}LinearLayout metrics=row();metrics.addView(metricCard("SERVIÇOS",filtered.size(),v->showEntriesDialog("Serviços • "+month,filtered)),new LinearLayout.LayoutParams(0,dp(92),1));gap(metrics);metrics.addView(metricCard("INSTALAÇÕES",installations.size(),v->showEntriesDialog("Instalações • "+month,installations)),new LinearLayout.LayoutParams(0,dp(92),1));gap(metrics);metrics.addView(metricCard("PLACAS",plates.size(),v->showEntriesDialog("Placas • "+month,filtered)),new LinearLayout.LayoutParams(0,dp(92),1));holder.addView(metrics,mb(12));renderCalendar61(holder,month,filtered);
    }

    LinearLayout metricCard(String label,int value,View.OnClickListener l){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setGravity(Gravity.CENTER);c.setPadding(dp(5),dp(8),dp(5),dp(8));c.setBackground(round(theme().surface,16,theme().stroke,1));c.setOnClickListener(l);c.setClickable(true);TextView n=txt(String.valueOf(value),25,theme().accent,true);n.setGravity(Gravity.CENTER);c.addView(n);TextView t=txt(label,9,themeMuted(),true);t.setGravity(Gravity.CENTER);c.addView(t);return c;}

    void renderCalendar61(LinearLayout holder,String month,ArrayList<ServiceStore.Entry> filtered){
        String[] p=month.split("/");if(p.length!=2)return;int m=Integer.parseInt(p[0])-1,y=Integer.parseInt(p[1]);Calendar cal=Calendar.getInstance();cal.clear();cal.set(y,m,1);int days=cal.getActualMaximum(Calendar.DAY_OF_MONTH);int offset=cal.get(Calendar.DAY_OF_WEEK)-1;LinearLayout box=card();box.addView(txt("CALENDÁRIO • "+month,14,themeText(),true),mb(10));GridLayout grid=new GridLayout(this);grid.setColumnCount(7);String[] wd={"D","S","T","Q","Q","S","S"};for(String w:wd){TextView x=txt(w,10,themeMuted(),true);x.setGravity(Gravity.CENTER);grid.addView(x,new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED,1f),GridLayout.spec(GridLayout.UNDEFINED,1f)));}for(int i=0;i<offset;i++){TextView blank=txt("",10,themeMuted(),false);grid.addView(blank,new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED,1f),GridLayout.spec(GridLayout.UNDEFINED,1f)));}
        SimpleDateFormat df=new SimpleDateFormat("dd/MM/yyyy",Locale.getDefault());for(int d=1;d<=days;d++){final int day=d;ArrayList<ServiceStore.Entry> dayEntries=new ArrayList<>();Calendar dc=Calendar.getInstance();dc.clear();dc.set(y,m,d);String key=df.format(dc.getTime());for(ServiceStore.Entry e:filtered)if(e.epoch>0&&key.equals(df.format(new Date(e.epoch))))dayEntries.add(e);TextView cell=txt(dayEntries.isEmpty()?String.valueOf(d):d+"\n"+dayEntries.size()+" serv.",dayEntries.isEmpty()?11:10,dayEntries.isEmpty()?themeMuted():themeText(),!dayEntries.isEmpty());cell.setGravity(Gravity.CENTER);cell.setMinHeight(dp(54));cell.setBackground(round(dayEntries.isEmpty()?theme().surface2:theme().surface,10,dayEntries.isEmpty()?theme().stroke:theme().accent,1));if(!dayEntries.isEmpty())cell.setOnClickListener(v->showEntriesDialog(String.format(Locale.getDefault(),"%02d/%02d/%04d",day,m+1,y),dayEntries));GridLayout.LayoutParams gp=new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED,1f),GridLayout.spec(GridLayout.UNDEFINED,1f));gp.setMargins(dp(2),dp(2),dp(2),dp(2));grid.addView(cell,gp);}box.addView(grid);holder.addView(box,mb(12));
    }

    void showEntriesDialog(String title,ArrayList<ServiceStore.Entry> entries){if(entries.isEmpty()){Toast.makeText(this,"Nenhum serviço neste filtro",Toast.LENGTH_SHORT).show();return;}String[] items=new String[entries.size()];for(int i=0;i<entries.size();i++){ServiceStore.Entry e=entries.get(i);items[i]=e.plate+" • "+e.service+" • "+e.technician;}new AlertDialog.Builder(this).setTitle(title).setItems(items,(d,w)->showServiceDialog(entries.get(w))).setNegativeButton("Fechar",null).show();}

    void startBackupExport(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/zip");i.putExtra(Intent.EXTRA_TITLE,"TAKI_FIELD_BACKUP_"+new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(new Date())+".zip");startActivityForResult(i,REQ_BACKUP_EXPORT);}
    void confirmRestore(){new AlertDialog.Builder(this).setTitle("Restaurar backup?").setMessage("O histórico e as preferências atuais serão substituídos pelos dados do backup selecionado. PDFs arquivados do backup serão restaurados.").setNegativeButton("Cancelar",null).setPositiveButton("SELECIONAR BACKUP",(d,w)->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/zip");startActivityForResult(i,REQ_BACKUP_IMPORT);}).show();}

    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri u=data.getData();if(req==REQ_BACKUP_EXPORT){Toast.makeText(this,"Gerando backup...",Toast.LENGTH_SHORT).show();new Thread(()->{try{BackupStore.Result r=BackupStore.exportZip(this,u,prefs);runOnUiThread(()->Toast.makeText(this,"Backup concluído • "+r.reports+" PDFs",Toast.LENGTH_LONG).show());}catch(Exception e){runOnUiThread(()->Toast.makeText(this,"Falha no backup: "+e.getMessage(),Toast.LENGTH_LONG).show());}}).start();}else if(req==REQ_BACKUP_IMPORT){Toast.makeText(this,"Restaurando backup...",Toast.LENGTH_SHORT).show();new Thread(()->{try{BackupStore.Result r=BackupStore.importZip(this,u,prefs);runOnUiThread(()->{Toast.makeText(this,"Backup restaurado • "+r.reports+" PDFs",Toast.LENGTH_LONG).show();recreate();});}catch(Exception e){runOnUiThread(()->Toast.makeText(this,"Falha ao restaurar: "+e.getMessage(),Toast.LENGTH_LONG).show());}}).start();}}

    @Override void checkUpdate(){
        Toast.makeText(this,"Consultando versão...",Toast.LENGTH_SHORT).show();new Thread(()->{try{URL u=new URL("https://raw.githubusercontent.com/SrRafa13/TAKI_FIELD_ANDROID/main/version.txt");BufferedReader br=new BufferedReader(new InputStreamReader(u.openStream()));String latest=br.readLine();br.close();runOnUiThread(()->new AlertDialog.Builder(this).setTitle("TAKI FIELD • Atualização").setMessage("Versão instalada: "+VERSION_61+"\nVersão disponível: "+latest+(VERSION_61.equals(latest)?"\n\nSeu app está atualizado.":"\n\nHá uma versão diferente disponível.")).setNegativeButton("Fechar",null).setPositiveButton("ABRIR VERSÕES",(d,w)->openUrl("https://github.com/SrRafa13/TAKI_FIELD_ANDROID/actions")).show());}catch(Exception e){runOnUiThread(()->Toast.makeText(this,"Não foi possível consultar atualização",Toast.LENGTH_LONG).show());}}).start();
    }
}
