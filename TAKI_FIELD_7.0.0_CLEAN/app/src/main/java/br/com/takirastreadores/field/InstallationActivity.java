package br.com.takirastreadores.field;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.core.content.FileProvider;
import org.json.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class InstallationActivity extends Activity {
    static final int BG=Color.rgb(6,7,9), PANEL=Color.rgb(17,18,21), PANEL2=Color.rgb(25,26,30), RED=Color.rgb(238,35,48), RED2=Color.rgb(190,22,35), WHITE=Color.rgb(246,247,249), MUTED=Color.rgb(155,158,166), GREEN=Color.rgb(55,205,111), AMBER=Color.rgb(255,184,72);
    static final int REQ_START=1001, REQ_END=1002, REQ_EXTRA_CAMERA=1003, REQ_EXTRA_PICK=1004;
    static final String DRAFT_FILE="installation_batch_v5.json";

    LinearLayout list, modeRow, batchPanel, actionsPanel;
    EditText plate, moduleNumber, notes;
    Spinner serviceType, moduleBrand;
    TextView startStatus, endStatus, extraStatus, batchStatus, autoSaveStatus;
    Button individualBtn, batchBtn;
    Uri startPhoto, endPhoto, pendingCameraUri;
    ArrayList<Uri> extras=new ArrayList<>();
    ArrayList<ServiceRecord> batchRecords=new ArrayList<>();
    int pendingCameraType=0;
    boolean batchMode=false, restoring=false;
    Handler saveHandler=new Handler(Looper.getMainLooper());
    Runnable saveRunnable=()->saveDraft(false);

    @Override public void onCreate(Bundle b){super.onCreate(b);build();list.post(this::offerDraftResume);}
    @Override protected void onPause(){super.onPause();if(batchMode)saveDraft(false);}
    @Override protected void onStop(){super.onStop();if(batchMode)saveDraft(false);}

    void build(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);
        list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(dp(16),dp(14),dp(16),dp(38));list.setBackgroundColor(BG);sc.addView(list);setContentView(sc);

        LinearLayout header=row();
        TextView back=txt("‹",30,WHITE,true);back.setGravity(Gravity.CENTER);back.setOnClickListener(v->finish());header.addView(back,new LinearLayout.LayoutParams(dp(46),dp(46)));
        LinearLayout titleBox=new LinearLayout(this);titleBox.setOrientation(LinearLayout.VERTICAL);titleBox.addView(txt("RELATÓRIOS DE INSTALAÇÃO",22,WHITE,true));titleBox.addView(txt("TAKI FIELD • V5",11,RED,true));header.addView(titleBox,new LinearLayout.LayoutParams(0,-2,1));
        TextView badge=txt("CAMPO",10,WHITE,true);badge.setGravity(Gravity.CENTER);badge.setBackground(round(PANEL2,99,Color.rgb(48,50,57),1));header.addView(badge,new LinearLayout.LayoutParams(dp(64),dp(34)));list.addView(header,mb(14));

        LinearLayout mode=card();mode.addView(label("MODO DO RELATÓRIO"),mb(9));
        modeRow=row();individualBtn=segment("INDIVIDUAL",true,v->setMode(false));batchBtn=segment("EM LOTE",false,v->setMode(true));modeRow.addView(individualBtn,new LinearLayout.LayoutParams(0,dp(48),1));gap(modeRow);modeRow.addView(batchBtn,new LinearLayout.LayoutParams(0,dp(48),1));mode.addView(modeRow);
        list.addView(mode,mb(12));

        batchPanel=card();
        LinearLayout bt=row();bt.addView(txt("LOTE EM ANDAMENTO",15,WHITE,true),new LinearLayout.LayoutParams(0,-2,1));autoSaveStatus=txt("● AUTO-SAVE",10,GREEN,true);bt.addView(autoSaveStatus);batchPanel.addView(bt,mb(5));
        batchStatus=txt("0 placas concluídas • próxima placa #1",13,MUTED,false);batchPanel.addView(batchStatus,mb(10));
        LinearLayout br=row();br.addView(button("SALVAR AGORA",false,v->saveDraft(true)),new LinearLayout.LayoutParams(0,dp(46),1));gap(br);br.addView(button("VER PLACAS",false,v->showBatchList()),new LinearLayout.LayoutParams(0,dp(46),1));batchPanel.addView(br,mb(8));
        LinearLayout br2=row();br2.addView(button("RETOMAR SALVO",false,v->resumeDraft()),new LinearLayout.LayoutParams(0,dp(46),1));gap(br2);br2.addView(button("DESFAZER ÚLTIMA",false,v->undoLast()),new LinearLayout.LayoutParams(0,dp(46),1));batchPanel.addView(br2);
        list.addView(batchPanel,mb(12));

        LinearLayout info=card();
        LinearLayout section=row();section.addView(dot(RED));section.addView(txt("DADOS DO VEÍCULO",16,WHITE,true),new LinearLayout.LayoutParams(0,-2,1));TextView required=txt("PLACA OBRIGATÓRIA",9,AMBER,true);section.addView(required);info.addView(section,mb(12));
        info.addView(label("PLACA DO VEÍCULO"),mb(5));plate=input("ABC1D23");plate.setAllCaps(true);info.addView(plate,mb(12));
        info.addView(label("TIPO DE SERVIÇO"),mb(5));serviceType=spinner(new String[]{"Instalação","Troca","Retirada","Manutenção"});info.addView(serviceType,mb(12));
        info.addView(label("MÓDULO USADO (OPCIONAL)"),mb(5));moduleBrand=spinner(new String[]{"Não informar","Suntech","X3Tech","Queclink","Quanta","Outro"});info.addView(moduleBrand,mb(8));
        moduleNumber=input("Numeração / ESN / IMEI (opcional)");info.addView(moduleNumber);list.addView(info,mb(12));

        LinearLayout photos=card();
        photos.addView(txt("EVIDÊNCIAS FOTOGRÁFICAS",16,WHITE,true),mb(4));photos.addView(txt("Registre o painel no início e o resultado final. Fotos extras entram no mesmo relatório.",12,MUTED,false),mb(12));
        photos.addView(photoButton("01  •  INÍCIO FOTO",v->takePhoto(REQ_START)),new LinearLayout.LayoutParams(-1,dp(54)));startStatus=statusText("Nenhuma foto de início");photos.addView(startStatus,mb(11));
        photos.addView(photoButton("02  •  FIM FOTO",v->takePhoto(REQ_END)),new LinearLayout.LayoutParams(-1,dp(54)));endStatus=statusText("Nenhuma foto de fim");photos.addView(endStatus,mb(13));
        photos.addView(label("FOTOS ADICIONAIS"),mb(7));LinearLayout rr=row();rr.addView(button("ABRIR CÂMERA",false,v->takePhoto(REQ_EXTRA_CAMERA)),new LinearLayout.LayoutParams(0,dp(48),1));gap(rr);rr.addView(button("ANEXAR",false,v->pickExtra()),new LinearLayout.LayoutParams(0,dp(48),1));photos.addView(rr);extraStatus=statusText("0 fotos adicionais");photos.addView(extraStatus,mb(2));list.addView(photos,mb(12));

        LinearLayout obs=card();obs.addView(label("OBSERVAÇÕES"),mb(6));notes=input("Observações do atendimento...");notes.setSingleLine(false);notes.setMinLines(5);notes.setGravity(Gravity.TOP);notes.setPadding(dp(14),dp(13),dp(14),dp(13));obs.addView(notes);list.addView(obs,mb(12));

        actionsPanel=new LinearLayout(this);actionsPanel.setOrientation(LinearLayout.VERTICAL);list.addView(actionsPanel);
        bindAutosave();setMode(false);
    }

    void bindAutosave(){
        TextWatcher w=new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int d){}public void onTextChanged(CharSequence s,int a,int b,int c){scheduleSave();}public void afterTextChanged(Editable e){}};
        plate.addTextChangedListener(w);moduleNumber.addTextChangedListener(w);notes.addTextChangedListener(w);
        AdapterView.OnItemSelectedListener sl=new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){scheduleSave();}};
        serviceType.setOnItemSelectedListener(sl);moduleBrand.setOnItemSelectedListener(sl);
    }

    void setMode(boolean batch){
        batchMode=batch;individualBtn.setBackground(segmentBg(!batch));batchBtn.setBackground(segmentBg(batch));
        individualBtn.setTextColor(!batch?WHITE:MUTED);batchBtn.setTextColor(batch?WHITE:MUTED);batchPanel.setVisibility(batch?View.VISIBLE:View.GONE);renderActions();updateBatchStatus();if(batch)scheduleSave();
    }

    void renderActions(){actionsPanel.removeAllViews();
        if(!batchMode){actionsPanel.addView(button("GERAR PDF INDIVIDUAL",true,v->generateIndividual()),new LinearLayout.LayoutParams(-1,dp(58)));return;}
        TextView helper=txt("Finalize uma placa e siga para a próxima. O lote inteiro será entregue em um único PDF.",12,MUTED,false);actionsPanel.addView(helper,mb(9));
        actionsPanel.addView(button("FINALIZAR PLACA",true,v->finishCurrentPlate()),new LinearLayout.LayoutParams(-1,dp(58)));
        LinearLayout spacer=new LinearLayout(this);spacer.setMinimumHeight(dp(9));actionsPanel.addView(spacer);
        actionsPanel.addView(button("FINALIZAR LOTE • GERAR PDF ÚNICO",false,v->finalizeBatchRequest()),new LinearLayout.LayoutParams(-1,dp(56)));
        TextView saved=txt("Seu lote é salvo automaticamente quando você sai do app. O botão “Salvar agora” força uma gravação imediata.",11,MUTED,false);saved.setGravity(Gravity.CENTER);actionsPanel.addView(saved,mtWrap(10));
    }

    void takePhoto(int req){
        try{File dir=new File(getFilesDir(),"install_photos");dir.mkdirs();File f=new File(dir,"photo_"+System.currentTimeMillis()+".jpg");pendingCameraUri=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",f);pendingCameraType=req;Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);i.putExtra(MediaStore.EXTRA_OUTPUT,pendingCameraUri);i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivityForResult(i,req);}catch(Exception e){Toast.makeText(this,"Não foi possível abrir a câmera",Toast.LENGTH_LONG).show();}
    }
    void pickExtra(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);i.setAction(Intent.ACTION_OPEN_DOCUMENT);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,REQ_EXTRA_PICK);}

    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(result!=RESULT_OK)return;
        if(req==REQ_START){startPhoto=pendingCameraUri;markOk(startStatus,"Foto de início adicionada");}
        else if(req==REQ_END){endPhoto=pendingCameraUri;markOk(endStatus,"Foto de fim adicionada");}
        else if(req==REQ_EXTRA_CAMERA){extras.add(pendingCameraUri);updateExtra();}
        else if(req==REQ_EXTRA_PICK&&data!=null){
            if(data.getClipData()!=null){for(int j=0;j<data.getClipData().getItemCount();j++){Uri u=data.getClipData().getItemAt(j).getUri();persistUri(u);extras.add(u);}}
            else if(data.getData()!=null){Uri u=data.getData();persistUri(u);extras.add(u);}updateExtra();
        }scheduleSave();
    }
    void persistUri(Uri u){try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}}
    void markOk(TextView v,String s){v.setText("✓  "+s);v.setTextColor(GREEN);}
    void updateExtra(){extraStatus.setText(extras.size()+" fotos adicionais");extraStatus.setTextColor(extras.isEmpty()?MUTED:GREEN);}

    void generateIndividual(){
        String p=cleanPlate();if(p.isEmpty()){plate.setError("Informe a placa");return;}
        ServiceRecord r=captureCurrent();
        if(startPhoto==null||endPhoto==null){confirmMissingPhotos(()->generateSinglePdf(r));return;}generateSinglePdf(r);
    }

    void finishCurrentPlate(){
        String p=cleanPlate();if(p.isEmpty()){plate.setError("Informe a placa");return;}
        if(startPhoto==null||endPhoto==null){confirmMissingPhotos(this::commitCurrentPlate);return;}commitCurrentPlate();
    }

    void commitCurrentPlate(){
        ServiceRecord r=captureCurrent();batchRecords.add(r);int keepService=serviceType.getSelectedItemPosition(),keepBrand=moduleBrand.getSelectedItemPosition();clearCurrent();serviceType.setSelection(keepService);moduleBrand.setSelection(keepBrand);saveDraft(false);updateBatchStatus();
        new AlertDialog.Builder(this).setTitle("Placa adicionada ✓").setMessage(r.plate+" entrou no lote.\n\nO que deseja fazer agora?").setNegativeButton("FINALIZAR LOTE",(d,w)->generateBatchPdf()).setPositiveButton("PRÓXIMA PLACA",(d,w)->{plate.requestFocus();Toast.makeText(this,"Próxima placa #"+(batchRecords.size()+1),Toast.LENGTH_SHORT).show();}).show();
    }

    void finalizeBatchRequest(){
        if(!cleanPlate().isEmpty()){
            new AlertDialog.Builder(this).setTitle("Placa atual ainda aberta").setMessage("Deseja incluir "+cleanPlate()+" no lote antes de gerar o PDF?").setNegativeButton("Não incluir",(d,w)->generateBatchPdf()).setPositiveButton("Incluir e gerar",(d,w)->{
                if(startPhoto==null||endPhoto==null){confirmMissingPhotos(()->{batchRecords.add(captureCurrent());clearCurrent();saveDraft(false);generateBatchPdf();});}
                else{batchRecords.add(captureCurrent());clearCurrent();saveDraft(false);generateBatchPdf();}
            }).show();return;
        }generateBatchPdf();
    }

    void confirmMissingPhotos(Runnable go){new AlertDialog.Builder(this).setTitle("Fotos principais ausentes").setMessage("A foto de início e/ou fim ainda não foi adicionada. Deseja continuar mesmo assim?").setNegativeButton("Voltar",null).setPositiveButton("Continuar",(d,w)->go.run()).show();}

    ServiceRecord captureCurrent(){ServiceRecord r=new ServiceRecord();r.plate=cleanPlate();r.service=serviceType.getSelectedItem().toString();r.brand=moduleBrand.getSelectedItem().toString();r.module=moduleNumber.getText().toString().trim();r.notes=notes.getText().toString().trim();r.start=startPhoto==null?"":startPhoto.toString();r.end=endPhoto==null?"":endPhoto.toString();for(Uri u:extras)r.extras.add(u.toString());r.finishedAt=new SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(new Date());return r;}
    String cleanPlate(){return plate.getText().toString().trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9-]","");}

    void clearCurrent(){boolean wasRestoring=restoring;restoring=true;plate.setText("");moduleNumber.setText("");notes.setText("");startPhoto=null;endPhoto=null;extras.clear();startStatus.setText("Nenhuma foto de início");startStatus.setTextColor(MUTED);endStatus.setText("Nenhuma foto de fim");endStatus.setTextColor(MUTED);updateExtra();restoring=wasRestoring;if(!restoring)scheduleSave();}

    void updateBatchStatus(){if(batchStatus==null)return;batchStatus.setText(batchRecords.size()+" placas concluídas • próxima placa #"+(batchRecords.size()+1));batchStatus.setTextColor(batchRecords.isEmpty()?MUTED:GREEN);}

    void scheduleSave(){if(restoring||!batchMode)return;saveHandler.removeCallbacks(saveRunnable);saveHandler.postDelayed(saveRunnable,450);}
    File draftFile(){return new File(getFilesDir(),DRAFT_FILE);}
    void saveDraft(boolean toast){if(!batchMode||restoring)return;try{JSONObject root=new JSONObject();root.put("version",5);root.put("mode","batch");JSONArray arr=new JSONArray();for(ServiceRecord r:batchRecords)arr.put(r.toJson());root.put("records",arr);root.put("current",captureCurrent().toJson());root.put("serviceIndex",serviceType.getSelectedItemPosition());root.put("brandIndex",moduleBrand.getSelectedItemPosition());root.put("savedAt",System.currentTimeMillis());try(FileOutputStream fos=new FileOutputStream(draftFile())){fos.write(root.toString().getBytes("UTF-8"));}if(autoSaveStatus!=null){autoSaveStatus.setText("● SALVO");autoSaveStatus.setTextColor(GREEN);}if(toast)Toast.makeText(this,"Andamento salvo no aparelho",Toast.LENGTH_SHORT).show();}catch(Exception e){if(autoSaveStatus!=null){autoSaveStatus.setText("● ERRO AO SALVAR");autoSaveStatus.setTextColor(RED);}if(toast)Toast.makeText(this,"Erro ao salvar: "+e.getMessage(),Toast.LENGTH_LONG).show();}}

    JSONObject readDraft(){try{File f=draftFile();if(!f.exists())return null;ByteArrayOutputStream b=new ByteArrayOutputStream();try(FileInputStream in=new FileInputStream(f)){byte[] buf=new byte[4096];int n;while((n=in.read(buf))>0)b.write(buf,0,n);}return new JSONObject(new String(b.toByteArray(),"UTF-8"));}catch(Exception e){return null;}}
    void offerDraftResume(){JSONObject d=readDraft();if(d==null)return;int count=d.optJSONArray("records")==null?0:d.optJSONArray("records").length();new AlertDialog.Builder(this).setTitle("Lote salvo encontrado").setMessage("Há um atendimento em lote salvo neste aparelho com "+count+" placas concluídas.\n\nDeseja continuar de onde parou?").setNegativeButton("Agora não",null).setPositiveButton("CONTINUAR",(x,w)->restoreDraft(d)).show();}
    void resumeDraft(){JSONObject d=readDraft();if(d==null){Toast.makeText(this,"Nenhum lote salvo",Toast.LENGTH_SHORT).show();return;}restoreDraft(d);}

    void restoreDraft(JSONObject root){try{restoring=true;setMode(true);batchRecords.clear();JSONArray arr=root.optJSONArray("records");if(arr!=null)for(int i=0;i<arr.length();i++)batchRecords.add(ServiceRecord.fromJson(arr.getJSONObject(i)));JSONObject cur=root.optJSONObject("current");if(cur!=null)applyCurrent(ServiceRecord.fromJson(cur));serviceType.setSelection(Math.max(0,Math.min(root.optInt("serviceIndex",0),serviceType.getCount()-1)));moduleBrand.setSelection(Math.max(0,Math.min(root.optInt("brandIndex",0),moduleBrand.getCount()-1)));updateBatchStatus();restoring=false;Toast.makeText(this,"Lote retomado",Toast.LENGTH_SHORT).show();}catch(Exception e){restoring=false;Toast.makeText(this,"Não foi possível restaurar o lote",Toast.LENGTH_LONG).show();}}
    void applyCurrent(ServiceRecord r){plate.setText(r.plate);moduleNumber.setText(r.module);notes.setText(r.notes);startPhoto=uriOrNull(r.start);endPhoto=uriOrNull(r.end);extras.clear();for(String s:r.extras){Uri u=uriOrNull(s);if(u!=null)extras.add(u);}if(startPhoto!=null)markOk(startStatus,"Foto de início restaurada");else{startStatus.setText("Nenhuma foto de início");startStatus.setTextColor(MUTED);}if(endPhoto!=null)markOk(endStatus,"Foto de fim restaurada");else{endStatus.setText("Nenhuma foto de fim");endStatus.setTextColor(MUTED);}updateExtra();selectSpinner(serviceType,r.service);selectSpinner(moduleBrand,r.brand);}
    Uri uriOrNull(String s){try{return s==null||s.isEmpty()?null:Uri.parse(s);}catch(Exception e){return null;}}
    void selectSpinner(Spinner s,String val){if(val==null)return;for(int i=0;i<s.getCount();i++)if(val.equals(s.getItemAtPosition(i).toString())){s.setSelection(i);return;}}

    void undoLast(){if(batchRecords.isEmpty()){Toast.makeText(this,"Nenhuma placa para desfazer",Toast.LENGTH_SHORT).show();return;}ServiceRecord r=batchRecords.remove(batchRecords.size()-1);applyCurrent(r);saveDraft(false);updateBatchStatus();Toast.makeText(this,r.plate+" voltou para edição",Toast.LENGTH_SHORT).show();}
    void showBatchList(){if(batchRecords.isEmpty()){Toast.makeText(this,"O lote ainda não tem placas finalizadas",Toast.LENGTH_SHORT).show();return;}StringBuilder s=new StringBuilder();for(int i=0;i<batchRecords.size();i++){ServiceRecord r=batchRecords.get(i);s.append(i+1).append(". ").append(r.plate).append(" • ").append(r.service);if(!r.module.isEmpty())s.append(" • ").append(r.module);s.append("\n");}new AlertDialog.Builder(this).setTitle("Placas do lote • "+batchRecords.size()).setMessage(s.toString()).setPositiveButton("OK",null).show();}

    void generateSinglePdf(ServiceRecord r){try{PdfDocument doc=new PdfDocument();writeRecord(doc,r,1,false);String filename="TAKI_"+safe(r.service)+"_"+r.plate+"_"+stamp()+".pdf";Uri out=savePdf(doc,filename);doc.close();showDone(filename,out,false);}catch(Exception e){Toast.makeText(this,"Erro ao gerar PDF: "+e.getMessage(),Toast.LENGTH_LONG).show();}}

    void generateBatchPdf(){if(batchRecords.isEmpty()){Toast.makeText(this,"Finalize pelo menos uma placa para gerar o lote",Toast.LENGTH_LONG).show();return;}try{PdfDocument doc=new PdfDocument();int page=1;page=writeBatchCover(doc,page);for(ServiceRecord r:batchRecords)page=writeRecord(doc,r,page,true);String filename="TAKI_LOTE_"+batchRecords.size()+"_PLACAS_"+stamp()+".pdf";Uri out=savePdf(doc,filename);doc.close();saveHandler.removeCallbacks(saveRunnable);restoring=true;batchRecords.clear();clearCurrent();draftFile().delete();restoring=false;updateBatchStatus();showDone(filename,out,true);}catch(Exception e){Toast.makeText(this,"Erro ao gerar PDF do lote: "+e.getMessage(),Toast.LENGTH_LONG).show();}}

    int writeBatchCover(PdfDocument doc,int pageNo){PdfDocument.Page page=newPage(doc,pageNo++);Canvas c=page.getCanvas();drawTopBand(c);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(Color.rgb(25,25,25));p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(28);c.drawText("TAKI FIELD",48,72,p);p.setTextSize(17);p.setColor(RED);c.drawText("RELATÓRIO EM LOTE",48,105,p);Paint body=new Paint(Paint.ANTI_ALIAS_FLAG);body.setColor(Color.DKGRAY);body.setTextSize(13);c.drawText(batchRecords.size()+" veículos • gerado em "+new SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(new Date()),48,136,body);int y=180;Paint item=new Paint(Paint.ANTI_ALIAS_FLAG);item.setTextSize(14);item.setColor(Color.rgb(40,40,40));for(int i=0;i<batchRecords.size();i++){if(y>790){doc.finishPage(page);page=newPage(doc,pageNo++);c=page.getCanvas();drawTopBand(c);y=70;}ServiceRecord r=batchRecords.get(i);item.setTypeface(Typeface.DEFAULT_BOLD);c.drawText(String.format(Locale.ROOT,"%02d",i+1)+"   "+r.plate,48,y,item);item.setTypeface(Typeface.DEFAULT);c.drawText(r.service+(r.module.isEmpty()?"":" • "+r.module),210,y,item);y+=27;}doc.finishPage(page);return pageNo;}

    int writeRecord(PdfDocument doc,ServiceRecord r,int pageNo,boolean batch){PdfDocument.Page page=newPage(doc,pageNo++);Canvas c=page.getCanvas();int y=drawHeader(c,r.plate,48,batch?"RELATÓRIO EM LOTE":"RELATÓRIO DE ATENDIMENTO");Paint text=new Paint(Paint.ANTI_ALIAS_FLAG);text.setColor(Color.DKGRAY);text.setTextSize(14);text.setTypeface(Typeface.DEFAULT_BOLD);Paint body=new Paint(Paint.ANTI_ALIAS_FLAG);body.setColor(Color.DKGRAY);body.setTextSize(13);y+=18;c.drawText("Tipo de serviço: "+r.service,48,y,text);y+=24;if(!r.brand.equals("Não informar")||!r.module.isEmpty()){c.drawText("Módulo: "+(r.brand.equals("Não informar")?"":r.brand)+(r.module.isEmpty()?"":" - "+r.module),48,y,text);y+=24;}if(r.finishedAt!=null&&!r.finishedAt.isEmpty()){c.drawText("Finalizado em: "+r.finishedAt,48,y,body);y+=27;}if(r.notes!=null&&!r.notes.isEmpty()){c.drawText("Observações:",48,y,text);y+=20;y=drawWrapped(c,r.notes,48,y,500,body);y+=16;}doc.finishPage(page);
        ArrayList<PhotoItem> imgs=new ArrayList<>();if(!r.start.isEmpty())imgs.add(new PhotoItem("INÍCIO - PAINEL / CHAVE LIGADA",Uri.parse(r.start)));if(!r.end.isEmpty())imgs.add(new PhotoItem("FIM - ATENDIMENTO CONCLUÍDO",Uri.parse(r.end)));for(int i=0;i<r.extras.size();i++)imgs.add(new PhotoItem("FOTO ADICIONAL "+(i+1),Uri.parse(r.extras.get(i))));for(PhotoItem it:imgs){page=newPage(doc,pageNo++);c=page.getCanvas();drawHeader(c,r.plate,48,batch?"RELATÓRIO EM LOTE":"RELATÓRIO DE ATENDIMENTO");Paint h=new Paint(Paint.ANTI_ALIAS_FLAG);h.setColor(Color.DKGRAY);h.setTextSize(15);h.setTypeface(Typeface.DEFAULT_BOLD);c.drawText(it.label,48,112,h);Bitmap bm=loadBitmap(it.uri,1600);if(bm!=null){Rect dst=fitRect(bm.getWidth(),bm.getHeight(),48,140,499,650);c.drawBitmap(bm,null,dst,null);bm.recycle();}else{Paint warn=new Paint(Paint.ANTI_ALIAS_FLAG);warn.setColor(Color.GRAY);warn.setTextSize(14);c.drawText("Foto indisponível no momento da geração.",48,170,warn);}doc.finishPage(page);}return pageNo;}

    Uri savePdf(PdfDocument doc,String filename)throws Exception{ContentValues cv=new ContentValues();cv.put(MediaStore.Downloads.DISPLAY_NAME,filename);cv.put(MediaStore.Downloads.MIME_TYPE,"application/pdf");cv.put(MediaStore.Downloads.RELATIVE_PATH,"Download/TAKI_FIELD");Uri out=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,cv);if(out==null)throw new IOException("Falha ao criar arquivo");try(OutputStream os=getContentResolver().openOutputStream(out)){doc.writeTo(os);}return out;}
    void showDone(String filename,Uri out,boolean batch){new AlertDialog.Builder(this).setTitle(batch?"Lote finalizado ✓":"PDF gerado ✓").setMessage(filename+"\n\nSalvo em Downloads/TAKI_FIELD").setNeutralButton("ABRIR",(d,w)->openPdf(out)).setNegativeButton("Fechar",null).setPositiveButton("COMPARTILHAR",(d,w)->sharePdf(out)).show();}
    void sharePdf(Uri u){Intent s=new Intent(Intent.ACTION_SEND);s.setType("application/pdf");s.putExtra(Intent.EXTRA_STREAM,u);s.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(s,"Compartilhar relatório"));}
    void openPdf(Uri u){try{Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(u,"application/pdf");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(i);}catch(Exception e){Toast.makeText(this,"Nenhum leitor de PDF disponível",Toast.LENGTH_SHORT).show();}}

    PdfDocument.Page newPage(PdfDocument d,int n){return d.startPage(new PdfDocument.PageInfo.Builder(595,842,n).create());}
    void drawTopBand(Canvas c){Paint red=new Paint();red.setColor(RED);c.drawRect(0,0,595,12,red);}
    int drawHeader(Canvas c,String p,int y,String subtitle){drawTopBand(c);Paint t=new Paint(Paint.ANTI_ALIAS_FLAG);t.setColor(Color.BLACK);t.setTextSize(24);t.setTypeface(Typeface.DEFAULT_BOLD);c.drawText("TAKI FIELD",48,y,t);Paint s=new Paint(Paint.ANTI_ALIAS_FLAG);s.setColor(Color.DKGRAY);s.setTextSize(14);c.drawText(subtitle+" • "+p,48,y+24,s);return y+44;}
    int drawWrapped(Canvas c,String text,int x,int y,int width,Paint p){String[] words=text.replace("\n"," \n ").split(" ");String line="";for(String w:words){if(w.equals("\n")){if(!line.isEmpty())c.drawText(line,x,y,p);line="";y+=18;continue;}String test=line.isEmpty()?w:line+" "+w;if(p.measureText(test)>width){if(!line.isEmpty())c.drawText(line,x,y,p);line=w;y+=18;}else line=test;}if(!line.isEmpty()){c.drawText(line,x,y,p);y+=18;}return y;}
    Bitmap loadBitmap(Uri u,int max){try(InputStream is=getContentResolver().openInputStream(u)){Bitmap b=BitmapFactory.decodeStream(is);if(b==null)return null;int m=Math.max(b.getWidth(),b.getHeight());if(m>max){float r=(float)max/m;Bitmap s=Bitmap.createScaledBitmap(b,Math.max(1,(int)(b.getWidth()*r)),Math.max(1,(int)(b.getHeight()*r)),true);b.recycle();return s;}return b;}catch(Exception e){return null;}}
    Rect fitRect(int sw,int sh,int x,int y,int w,int h){float r=Math.min((float)w/sw,(float)h/sh);int nw=(int)(sw*r),nh=(int)(sh*r);int dx=x+(w-nw)/2,dy=y+(h-nh)/2;return new Rect(dx,dy,dx+nw,dy+nh);}
    String safe(String s){return s.toUpperCase(Locale.ROOT).replace("Ç","C").replace("Ã","A").replace("Á","A").replace("É","E").replace("Í","I").replace("Ó","O").replace("Ú","U").replaceAll("[^A-Z0-9]+","_");}
    String stamp(){return new SimpleDateFormat("yyyyMMdd_HHmm",Locale.getDefault()).format(new Date());}

    static class ServiceRecord{String plate="",service="Instalação",brand="Não informar",module="",notes="",start="",end="",finishedAt="";ArrayList<String> extras=new ArrayList<>();JSONObject toJson()throws JSONException{JSONObject o=new JSONObject();o.put("plate",plate);o.put("service",service);o.put("brand",brand);o.put("module",module);o.put("notes",notes);o.put("start",start);o.put("end",end);o.put("finishedAt",finishedAt);JSONArray a=new JSONArray();for(String s:extras)a.put(s);o.put("extras",a);return o;}static ServiceRecord fromJson(JSONObject o){ServiceRecord r=new ServiceRecord();r.plate=o.optString("plate","");r.service=o.optString("service","Instalação");r.brand=o.optString("brand","Não informar");r.module=o.optString("module","");r.notes=o.optString("notes","");r.start=o.optString("start","");r.end=o.optString("end","");r.finishedAt=o.optString("finishedAt","");JSONArray a=o.optJSONArray("extras");if(a!=null)for(int i=0;i<a.length();i++)r.extras.add(a.optString(i));return r;}}
    static class PhotoItem{String label;Uri uri;PhotoItem(String l,Uri u){label=l;uri=u;}}

    LinearLayout card(){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setPadding(dp(16),dp(16),dp(16),dp(16));GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(21,22,26),Color.rgb(13,14,17)});g.setCornerRadius(dp(18));g.setStroke(dp(1),Color.rgb(37,39,45));x.setBackground(g);x.setElevation(dp(3));return x;}
    LinearLayout row(){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.HORIZONTAL);x.setGravity(Gravity.CENTER_VERTICAL);return x;}
    void gap(LinearLayout r){Space s=new Space(this);r.addView(s,new LinearLayout.LayoutParams(dp(8),1));}
    View dot(int color){TextView d=new TextView(this);d.setBackground(round(color,99,color,0));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(8),dp(8));p.rightMargin=dp(9);d.setLayoutParams(p);return d;}
    TextView txt(String s,int size,int color,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextColor(color);v.setTextSize(size);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    TextView label(String s){TextView v=txt(s,11,MUTED,true);v.setLetterSpacing(.08f);return v;}
    TextView statusText(String s){TextView v=txt(s,12,MUTED,false);v.setPadding(dp(4),dp(7),0,0);return v;}
    EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(Color.rgb(105,108,116));e.setTextColor(WHITE);e.setTextSize(15);e.setSingleLine(true);e.setBackground(round(PANEL2,13,Color.rgb(48,50,57),1));e.setPadding(dp(14),0,dp(14),0);e.setMinHeight(dp(52));return e;}
    Spinner spinner(String[] a){Spinner s=new Spinner(this);ArrayAdapter<String> ad=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,a){@Override public View getView(int p,View c,ViewGroup g){TextView v=(TextView)super.getView(p,c,g);v.setTextColor(WHITE);v.setTextSize(15);v.setPadding(dp(10),0,dp(10),0);return v;}@Override public View getDropDownView(int p,View c,ViewGroup g){TextView v=(TextView)super.getDropDownView(p,c,g);v.setTextColor(WHITE);v.setBackgroundColor(PANEL2);v.setPadding(dp(16),dp(14),dp(16),dp(14));return v;}};s.setAdapter(ad);s.setBackground(round(PANEL2,13,Color.rgb(48,50,57),1));s.setMinimumHeight(dp(52));return s;}
    Button button(String s,boolean red,View.OnClickListener l){Button b=new Button(this);b.setText(s);b.setTextColor(WHITE);b.setTextSize(12);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setAllCaps(false);b.setBackground(round(red?RED:PANEL2,14,red?RED2:Color.rgb(54,56,64),1));b.setOnClickListener(l);b.setStateListAnimator(null);return b;}
    Button photoButton(String s,View.OnClickListener l){Button b=button(s,true,l);b.setGravity(Gravity.CENTER_VERTICAL);b.setPadding(dp(18),0,dp(18),0);return b;}
    Button segment(String s,boolean selected,View.OnClickListener l){Button b=new Button(this);b.setText(s);b.setTextSize(12);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setAllCaps(false);b.setTextColor(selected?WHITE:MUTED);b.setBackground(segmentBg(selected));b.setOnClickListener(l);b.setStateListAnimator(null);return b;}
    GradientDrawable segmentBg(boolean selected){return round(selected?RED:PANEL2,14,selected?RED2:Color.rgb(52,54,61),1);}
    GradientDrawable round(int color,int radius,int strokeColor,int stroke){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));if(stroke>0)g.setStroke(dp(stroke),strokeColor);return g;}
    LinearLayout.LayoutParams mb(int x){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(x);return p;}
    LinearLayout.LayoutParams mtWrap(int x){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(x);return p;}
    int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}
