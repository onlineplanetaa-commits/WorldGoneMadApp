package info.uastena.worldgonemad;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final String site = "https://ua-stena.info";
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private LinearLayout root, list;
    private TextView status;
    private String language = "en", searchTerm = "";
    private final int red = Color.rgb(220,25,45), black = Color.rgb(8,8,12), panel = Color.rgb(23,20,25);

    private String tr(String key) {
        String[] en = {"UFOs • paranormal • wars • hidden history","ENTER THE UNKNOWN","UFOs & ALIENS","WITCHES & PARANORMAL","WARS & DISASTERS","DARK POLITICS","HIDDEN HISTORY","Latest articles","Loading articles…","Could not load articles. Check your connection.","Refresh","Read full article","← Back to articles","No articles found."};
        String[] ru = {"Тайны мира • НЛО • потустороннее • скрытая история","ТЕРРИТОРИЯ НЕИЗВЕДАННОГО","НЛО И ПРИШЕЛЬЦЫ","ВЕДЬМЫ И ПОТУСТОРОННЕЕ","ВОЙНЫ И КАТАСТРОФЫ","ТЁМНАЯ ПОЛИТИКА","ТАЙНЫ ИСТОРИИ","Последние публикации","Загружаем публикации…","Не удалось загрузить публикации. Проверьте интернет.","Обновить","Читать полностью на сайте","← К публикациям","Публикаций пока нет."};
        String[] uk = {"Таємниці світу • НЛО • потойбіччя • прихована історія","ТЕРИТОРІЯ НЕЗНАНОГО","НЛО ТА ПРИБУЛЬЦІ","ВІДЬМИ Й ПОТОЙБІЧЧЯ","ВІЙНИ Й КАТАСТРОФИ","ТЕМНА ПОЛІТИКА","ТАЄМНИЦІ ІСТОРІЇ","Останні публікації","Завантажуємо публікації…","Не вдалося завантажити публікації. Перевірте інтернет.","Оновити","Читати повністю на сайті","← До публікацій","Публікацій поки немає."};
        String[] a = language.equals("ru") ? ru : language.equals("uk") ? uk : en;
        String[] keys = {"subtitle","categories","ufo","paranormal","war","politics","history","latest","loading","error","retry","open","back","empty"};
        for (int i=0;i<keys.length;i++) if(keys[i].equals(key)) return a[i];
        return key;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(black);
        getWindow().setNavigationBarColor(black);
        showHome();
    }

    private int dp(float n) { return (int)(n*getResources().getDisplayMetrics().density+0.5f); }
    private GradientDrawable shape(int color,int radius,int stroke) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius));
        if(stroke!=0)d.setStroke(dp(1),stroke); return d;
    }
    private TextView text(String s,int size,int color) {
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); return t;
    }
    private Button button(String s,Runnable action) {
        Button b=new Button(this); b.setText(s); b.setAllCaps(false); b.setTextColor(Color.WHITE); b.setTextSize(14);
        b.setBackground(shape(Color.rgb(34,14,19),8,red)); b.setPadding(dp(10),dp(3),dp(10),dp(3));
        b.setOnClickListener(v->action.run()); return b;
    }
    private void base(boolean scroll) {
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(black);
        root.setPadding(dp(16),dp(10),dp(16),dp(10)); setContentView(root);
        LinearLayout header=new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand=text("WORLD GONE MAD",21,Color.WHITE); brand.setTypeface(Typeface.DEFAULT,Typeface.BOLD); brand.setLetterSpacing(.04f);
        header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        header.addView(button("EN",()->setLanguage("en")));
        header.addView(button("RU",()->setLanguage("ru")));
        header.addView(button("UK",()->setLanguage("uk")));
        root.addView(header);
        TextView sub=text(tr("subtitle"),12,Color.rgb(230,80,92)); sub.setPadding(0,dp(5),0,dp(12)); root.addView(sub);
        if(scroll) {
            ScrollView sv=new ScrollView(this); list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
            sv.addView(list); root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        }
    }
    private void setLanguage(String lang) { language=lang; searchTerm=""; showHome(); }
    private void showHome() {
        base(true); addCategories();
        TextView h=text(tr("latest"),20,Color.WHITE); h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        h.setPadding(0,dp(8),0,dp(8)); list.addView(h);
        status=text(tr("loading"),15,Color.LTGRAY); status.setPadding(dp(4),dp(8),dp(4),dp(8)); list.addView(status);
        list.addView(button(tr("retry"),this::loadPosts)); loadPosts();
    }
    private void addCategories() {
        TextView label=text(tr("categories"),12,red); label.setTypeface(Typeface.DEFAULT,Typeface.BOLD); label.setLetterSpacing(.1f);
        label.setPadding(0,dp(6),0,dp(8)); list.addView(label);
        addCategoryRow(new String[]{"ufo","paranormal"}); addCategoryRow(new String[]{"war","politics"}); addCategoryRow(new String[]{"history"});
    }
    private void addCategoryRow(String[] keys) {
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2); rp.bottomMargin=dp(6); list.addView(row,rp);
        for(String key:keys) {
            Button b=button(tr(key),()->{searchTerm=categoryQuery(key);loadPosts();}); b.setTextSize(12); b.setMinHeight(dp(42)); b.setMaxLines(2);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1); p.rightMargin=dp(6); row.addView(b,p);
        }
    }
    private String categoryQuery(String key) {
        if(language.equals("ru")) { if(key.equals("ufo"))return "НЛО"; if(key.equals("paranormal"))return "ведьмы"; if(key.equals("war"))return "война"; if(key.equals("politics"))return "политика"; return "история"; }
        if(language.equals("uk")) { if(key.equals("ufo"))return "НЛО"; if(key.equals("paranormal"))return "відьми"; if(key.equals("war"))return "війна"; if(key.equals("politics"))return "політика"; return "історія"; }
        if(key.equals("ufo"))return "UFO"; if(key.equals("paranormal"))return "witches paranormal"; if(key.equals("war"))return "war disaster"; if(key.equals("politics"))return "politics"; return "hidden history";
    }
    private String apiUrl() {
        String prefix=language.equals("ru")?"/ru":language.equals("uk")?"/uk":"";
        String u=site+prefix+"/wp-json/wp/v2/posts?per_page=30&_embed=1&orderby=date&order=desc";
        if(!searchTerm.isEmpty())try{u+="&search="+URLEncoder.encode(searchTerm,"UTF-8");}catch(Exception ignored){}
        return u;
    }
    private void loadPosts() {
        if(status!=null)status.setText(tr("loading"));
        executor.execute(()->{
            HttpURLConnection c=null;
            try {
                c=(HttpURLConnection)new URL(apiUrl()).openConnection(); c.setConnectTimeout(12000); c.setReadTimeout(15000);
                c.setRequestProperty("Accept","application/json");
                InputStream in=c.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] buf=new byte[8192]; int n;
                while((n=in.read(buf))!=-1)out.write(buf,0,n); in.close();
                JSONArray arr=new JSONArray(new String(out.toByteArray(),"UTF-8")); main.post(()->renderPosts(arr));
            } catch(Exception e) {
                main.post(()->{if(status!=null)status.setText(tr("error")+"\n"+apiUrl());});
            } finally {if(c!=null)c.disconnect();}
        });
    }
    private String clean(String s) { return android.text.Html.fromHtml(s,android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim(); }
    private void renderPosts(JSONArray arr) {
        if(list==null)return; list.removeAllViews(); addCategories();
        TextView h=text(searchTerm.isEmpty()?tr("latest"):tr("categories"),20,Color.WHITE); h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        h.setPadding(0,dp(8),0,dp(8)); list.addView(h);
        if(arr.length()==0){list.addView(text(tr("empty"),15,Color.LTGRAY));return;}
        for(int i=0;i<arr.length();i++) {
            JSONObject post=arr.optJSONObject(i); if(post==null)continue;
            JSONObject titleObj=post.optJSONObject("title"), excerptObj=post.optJSONObject("excerpt");
            String title=clean(titleObj==null?"":titleObj.optString("rendered",""));
            String excerpt=clean(excerptObj==null?"":excerptObj.optString("rendered",""));
            String link=post.optString("link",site), image="";
            JSONObject emb=post.optJSONObject("_embedded");
            if(emb!=null){JSONArray media=emb.optJSONArray("wp:featuredmedia");if(media!=null&&media.length()>0&&media.optJSONObject(0)!=null)image=media.optJSONObject(0).optString("source_url","");}
            addPost(title,excerpt,link,image);
        }
    }
    private void addPost(String title,String excerpt,String link,String imageUrl) {
        LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(12),dp(12),dp(12),dp(12));
        card.setBackground(shape(panel,10,Color.rgb(74,24,34)));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2); cp.bottomMargin=dp(12); list.addView(card,cp);
        if(imageUrl.startsWith("https://")) {
            ImageView iv=new ImageView(this); iv.setScaleType(ImageView.ScaleType.CENTER_CROP); iv.setBackground(shape(Color.rgb(45,15,22),8,0));
            card.addView(iv,new LinearLayout.LayoutParams(-1,dp(180)));
            executor.execute(()->{try{HttpURLConnection c=(HttpURLConnection)new URL(imageUrl).openConnection();c.setConnectTimeout(8000);c.setReadTimeout(10000);InputStream in=c.getInputStream();Bitmap bmp=BitmapFactory.decodeStream(in);in.close();c.disconnect();if(bmp!=null)main.post(()->iv.setImageBitmap(bmp));}catch(Exception ignored){}});
        }
        TextView t=text(title.isEmpty()?"(Untitled)":title,19,Color.WHITE);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(0,dp(8),0,dp(5));card.addView(t);
        if(!excerpt.isEmpty()){TextView ex=text(excerpt,14,Color.rgb(205,211,220));ex.setMaxLines(4);card.addView(ex);}
        Button open=button(tr("open"),()->showArticle(link));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);bp.topMargin=dp(10);card.addView(open,bp);
        card.setFocusable(true);card.setOnClickListener(v->showArticle(link));
        card.setOnFocusChangeListener((v,focused)->card.setBackground(shape(focused?Color.rgb(70,18,29):panel,10,red)));
    }
    private void showArticle(String url) {
        base(false); root.addView(button(tr("back"),this::showHome));
        WebView web=new WebView(this); web.setBackgroundColor(black); web.getSettings().setJavaScriptEnabled(false);
        web.setWebViewClient(new WebViewClient()); root.addView(web,new LinearLayout.LayoutParams(-1,0,1)); web.loadUrl(url);
    }
    @Override public boolean onKeyDown(int keyCode,KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK){showHome();return true;} return super.onKeyDown(keyCode,event);
    }
    @Override protected void onDestroy(){super.onDestroy();executor.shutdownNow();}
}
