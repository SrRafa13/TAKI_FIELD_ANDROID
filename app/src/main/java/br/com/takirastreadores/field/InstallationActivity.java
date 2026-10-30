package br.com.takirastreadores.field;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import androidx.core.content.FileProvider;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class InstallationActivity extends Activity {
    LinearLayout list;
    EditText plate, moduleNumber, notes;
    Spinner serviceType, moduleBrand;
    TextView startStatus, endStatus, extraStatus;
    Uri startPhoto, endPhoto;
    ArrayList<Uri> extras = new ArrayList<>();
    Uri pendingCameraUri;
    int pendingCameraType = 0;
    static final int REQ_START=1001, REQ_END=1002, REQ_EXTRA_CAMERA=1003, REQ_EXTRA_PICK=1004;
    static final int BG=Color.rgb(8,8,8), PANEL=Color.rgb(17,17,17), RED=Color.rgb(245,34,45), WHITE=Color.rgb(246,246,246), MUTED=Color.rgb(160,160,160);

    @Override public void onCreate(Bundle b){super.onCreate(b);build();}
    void build(){ScrollView sc=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(dp(16),dp(16),dp(16),dp(36));list.setBackgroundColor(BG);sc.addView(list);setContentView(sc);
        TextView back=txt("‹  VOLTAR",13,RED,true);back.setOnClickListener(v->finish());list.addView(back,mb(12));
        list.addView(txt("INSTALAÇÕES",26,WHITE,true),mb(3));list.addView(txt("Registre o atendimento e gere um PDF pronto para enviar.",13,MUTED,false),mb(16));

        LinearLayout info=card();info.addView(label("PLACA DO VEÍCULO"));plate=input("ABC1D23");plate.setAllCaps(true);info.addView(plate,mb(10));
        info.addView(label("TIPO DE SERVIÇO"));serviceType=spinner(new String[]{"Instalação","Troca","Retirada","Manutenção"});info.addView(serviceType,mb(10));
        info.addView(label("MÓDULO USADO (OPCIONAL)"));moduleBrand=spinner(new String[]{"Não informar","Suntech","X3Tech","Queclink","Outro"});info.addView(moduleBrand,mb(8));
        moduleNumber=input("Numeração / ESN / IMEI (opcional)");info.addView(moduleNumber);list.addView(info,mb(12));

        LinearLayout photos=card();photos.addView(txt("FOTOS PRINCIPAIS",16,WHITE,true),mb(5));photos.addView(txt("Tire uma foto do painel com a chave ligada no início e outra ao finalizar.",12,MUTED,false),mb(10));
        photos.addView(button("📷  INÍCIO FOTO",true,v->takePhoto(REQ_START)),new LinearLayout.LayoutParams(-1,dp(50)));startStatus=txt("Nenhuma foto de início",12,MUTED,false);photos.addView(startStatus,mb(10));
        photos.addView(button("📷  FIM FOTO",true,v->takePhoto(REQ_END)),new LinearLayout.LayoutParams(-1,dp(50)));endStatus=txt("Nenhuma foto de fim",12,MUTED,false);photos.addView(endStatus,mb(12));
        photos.addView(txt("FOTOS ADICIONAIS",14,WHITE,true),mb(6));LinearLayout rr=row();rr.addView(button("CÂMERA",false,v->takePhoto(REQ_EXTRA_CAMERA)),new LinearLayout.LayoutParams(0,dp(48),1));gap(rr);rr.addView(button("ANEXAR",false,v->pickExtra()),new LinearLayout.LayoutParams(0,dp(48),1));photos.addView(rr);extraStatus=txt("0 fotos adicionais",12,MUTED,false);photos.addView(extraStatus,mb(2));list.addView(photos,mb(12));

        LinearLayout obs=card();obs.addView(label("OBSERVAÇÕES"));notes=input("Observações do atendimento...");notes.setSingleLine(false);notes.setMinLines(5);notes.setGravity(Gravity.TOP);obs.addView(notes);list.addView(obs,mb(14));

        list.addView(button("GERAR PDF",true,v->generatePdf()),new LinearLayout.LayoutParams(-1,dp(56)));
    }

    void takePhoto(int req){try{File dir=new File(getCacheDir(),"install_photos");dir.mkdirs();File f=new File(dir,"photo_"+System.currentTimeMillis()+".jpg");pendingCameraUri=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",f);pendingCameraType=req;Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);i.putExtra(MediaStore.EXTRA_OUTPUT,pendingCameraUri);i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivityForResult(i,req);}catch(Exception e){Toast.makeText(this,"Não foi possível abrir a câmera",Toast.LENGTH_LONG).show();}}
    void pickExtra(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);i.setAction(Intent.ACTION_OPEN_DOCUMENT);startActivityForResult(i,REQ_EXTRA_PICK);}

    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(result!=RESULT_OK)return;
        if(req==REQ_START){startPhoto=pendingCameraUri;startStatus.setText("✓ Foto de início adicionada");startStatus.setTextColor(Color.rgb(53,199,89));}
        else if(req==REQ_END){endPhoto=pendingCameraUri;endStatus.setText("✓ Foto de fim adicionada");endStatus.setTextColor(Color.rgb(53,199,89));}
        else if(req==REQ_EXTRA_CAMERA){extras.add(pendingCameraUri);updateExtra();}
        else if(req==REQ_EXTRA_PICK && data!=null){if(data.getClipData()!=null){for(int j=0;j<data.getClipData().getItemCount();j++){Uri u=data.getClipData().getItemAt(j).getUri();extras.add(u);try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}}}else if(data.getData()!=null){Uri u=data.getData();extras.add(u);try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}}updateExtra();}}
    void updateExtra(){extraStatus.setText(extras.size()+" fotos adicionais");}

    void generatePdf(){String p=plate.getText().toString().trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9-]","");if(p.isEmpty()){plate.setError("Informe a placa");return;}if(startPhoto==null||endPhoto==null){new AlertDialog.Builder(this).setTitle("Fotos principais ausentes").setMessage("Você ainda não adicionou a foto de início e/ou fim. Deseja gerar o PDF mesmo assim?").setNegativeButton("Voltar",null).setPositiveButton("Gerar mesmo assim",(d,w)->doGenerate(p)).show();return;}doGenerate(p);}

    void doGenerate(String p){try{PdfDocument doc=new PdfDocument();ArrayList<PhotoItem> imgs=new ArrayList<>();if(startPhoto!=null)imgs.add(new PhotoItem("INÍCIO - PAINEL / CHAVE LIGADA",startPhoto));if(endPhoto!=null)imgs.add(new PhotoItem("FIM - ATENDIMENTO CONCLUÍDO",endPhoto));for(int i=0;i<extras.size();i++)imgs.add(new PhotoItem("FOTO ADICIONAL "+(i+1),extras.get(i)));
        int pageNo=1;PdfDocument.Page page=newPage(doc,pageNo++);Canvas c=page.getCanvas();int y=drawHeader(c,p,48);
        Paint text=new Paint(Paint.ANTI_ALIAS_FLAG);text.setColor(Color.DKGRAY);text.setTextSize(14);text.setTypeface(Typeface.DEFAULT_BOLD);
        Paint body=new Paint(Paint.ANTI_ALIAS_FLAG);body.setColor(Color.DKGRAY);body.setTextSize(13);
        y+=18;c.drawText("Tipo de serviço: "+serviceType.getSelectedItem().toString(),48,y,text);y+=24;
        String mb=moduleBrand.getSelectedItem().toString();String mn=moduleNumber.getText().toString().trim();if(!mb.equals("Não informar")||!mn.isEmpty()){c.drawText("Módulo: "+(mb.equals("Não informar")?"":mb)+(mn.isEmpty()?"":" - "+mn),48,y,text);y+=24;}
        String date=new SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(new Date());c.drawText("Gerado em: "+date,48,y,body);y+=28;
        String obs=notes.getText().toString().trim();if(!obs.isEmpty()){c.drawText("Observações:",48,y,text);y+=20;y=drawWrapped(c,obs,48,y,500,body);y+=16;}
        doc.finishPage(page);
        for(PhotoItem item:imgs){page=newPage(doc,pageNo++);c=page.getCanvas();drawHeader(c,p,48);Paint h=new Paint(Paint.ANTI_ALIAS_FLAG);h.setColor(Color.DKGRAY);h.setTextSize(15);h.setTypeface(Typeface.DEFAULT_BOLD);c.drawText(item.label,48,112,h);Bitmap b=loadBitmap(item.uri,1500);if(b!=null){Rect dst=fitRect(b.getWidth(),b.getHeight(),48,140,499,650);c.drawBitmap(b,null,dst,null);b.recycle();}doc.finishPage(page);}
        String filename="TAKI_"+serviceType.getSelectedItem().toString().toUpperCase(Locale.ROOT).replace("Ç","C").replace("Ã","A").replace(" ","_")+"_"+p+"_"+new SimpleDateFormat("yyyyMMdd_HHmm",Locale.getDefault()).format(new Date())+".pdf";
        ContentValues cv=new ContentValues();cv.put(MediaStore.Downloads.DISPLAY_NAME,filename);cv.put(MediaStore.Downloads.MIME_TYPE,"application/pdf");cv.put(MediaStore.Downloads.RELATIVE_PATH,"Download/TAKI_FIELD");Uri out=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,cv);if(out==null)throw new IOException("Falha ao criar arquivo");OutputStream os=getContentResolver().openOutputStream(out);doc.writeTo(os);os.close();doc.close();showDone(filename,out);
    }catch(Exception e){Toast.makeText(this,"Erro ao gerar PDF: "+e.getMessage(),Toast.LENGTH_LONG).show();}}

    void showDone(String filename,Uri out){new AlertDialog.Builder(this).setTitle("PDF gerado ✓").setMessage(filename+"\n\nSalvo em Downloads/TAKI_FIELD").setNegativeButton("Fechar",null).setPositiveButton("Compartilhar",(d,w)->sharePdf(out)).show();}
    void sharePdf(Uri u){Intent s=new Intent(Intent.ACTION_SEND);s.setType("application/pdf");s.putExtra(Intent.EXTRA_STREAM,u);s.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(s,"Compartilhar relatório"));}

    PdfDocument.Page newPage(PdfDocument d,int n){PdfDocument.PageInfo info=new PdfDocument.PageInfo.Builder(595,842,n).create();return d.startPage(info);}
    int drawHeader(Canvas c,String p,int y){Paint red=new Paint();red.setColor(RED);c.drawRect(0,0,595,12,red);Paint t=new Paint(Paint.ANTI_ALIAS_FLAG);t.setColor(Color.BLACK);t.setTextSize(24);t.setTypeface(Typeface.DEFAULT_BOLD);c.drawText("TAKI FIELD",48,y,t);Paint s=new Paint(Paint.ANTI_ALIAS_FLAG);s.setColor(Color.DKGRAY);s.setTextSize(14);c.drawText("RELATÓRIO DE ATENDIMENTO • "+p,48,y+24,s);return y+44;}
    int drawWrapped(Canvas c,String text,int x,int y,int width,Paint p){String[] words=text.replace("\n"," \n ").split(" ");String line="";for(String w:words){if(w.equals("\n")){c.drawText(line,x,y,p);line="";y+=18;continue;}String test=line.isEmpty()?w:line+" "+w;if(p.measureText(test)>width){c.drawText(line,x,y,p);line=w;y+=18;}else line=test;}if(!line.isEmpty()){c.drawText(line,x,y,p);y+=18;}return y;}
    Bitmap loadBitmap(Uri u,int max){try{InputStream is=getContentResolver().openInputStream(u);Bitmap b=BitmapFactory.decodeStream(is);is.close();if(b==null)return null;int m=Math.max(b.getWidth(),b.getHeight());if(m>max){float r=(float)max/m;Bitmap s=Bitmap.createScaledBitmap(b,(int)(b.getWidth()*r),(int)(b.getHeight()*r),true);b.recycle();return s;}return b;}catch(Exception e){return null;}}
    Rect fitRect(int sw,int sh,int x,int y,int w,int h){float r=Math.min((float)w/sw,(float)h/sh);int nw=(int)(sw*r),nh=(int)(sh*r);int dx=x+(w-nw)/2,dy=y+(h-nh)/2;return new Rect(dx,dy,dx+nw,dy+nh);}
    static class PhotoItem{String label;Uri uri;PhotoItem(String l,Uri u){label=l;uri=u;}}

    LinearLayout card(){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setPadding(dp(16),dp(15),dp(16),dp(15));x.setBackgroundColor(PANEL);return x;}
    LinearLayout row(){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.HORIZONTAL);x.setGravity(Gravity.CENTER_VERTICAL);return x;}void gap(LinearLayout r){Space s=new Space(this);r.addView(s,new LinearLayout.LayoutParams(dp(8),1));}
    TextView txt(String s,int size,int color,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextColor(color);v.setTextSize(size);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}TextView label(String s){TextView v=txt(s,11,MUTED,true);v.setLetterSpacing(.08f);return v;}
    EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(Color.rgb(105,105,105));e.setTextColor(WHITE);e.setTextSize(15);e.setSingleLine(true);e.setBackgroundColor(Color.rgb(27,27,27));e.setPadding(dp(14),dp(10),dp(14),dp(10));e.setMinHeight(dp(52));return e;}
    Spinner spinner(String[] a){Spinner s=new Spinner(this);ArrayAdapter<String> ad=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,a){@Override public View getView(int p,View c,ViewGroup g){TextView v=(TextView)super.getView(p,c,g);v.setTextColor(WHITE);v.setTextSize(15);return v;}@Override public View getDropDownView(int p,View c,ViewGroup g){TextView v=(TextView)super.getDropDownView(p,c,g);v.setTextColor(WHITE);v.setBackgroundColor(Color.rgb(25,25,25));v.setPadding(dp(16),dp(14),dp(16),dp(14));return v;}};s.setAdapter(ad);s.setBackgroundColor(Color.rgb(27,27,27));s.setMinimumHeight(dp(52));return s;}
    Button button(String s,boolean red,View.OnClickListener l){Button b=new Button(this);b.setText(s);b.setTextColor(WHITE);b.setTextSize(12);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setAllCaps(false);b.setBackgroundColor(red?RED:Color.rgb(35,35,35));b.setOnClickListener(l);return b;}
    LinearLayout.LayoutParams mb(int x){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(x);return p;}int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}
