import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.concurrent.Executors.newScheduledThreadPool;

public class Main {
    private static final int COOKIES_COUNT = 2;
    private static final AtomicInteger counter = new AtomicInteger(0);

    private static final String COOKIE_NAME = "Cookie";
    private static final String URL = "https://www.mzv.cz/lvov/uk/x2004_02_03/x2016_05_18/x2017_11_24_1.html";
    private static final List<String> cookies = new ArrayList<String>(COOKIES_COUNT);

    private static final int THREAD_COUNT = 1;
    private static final ScheduledExecutorService executor = newScheduledThreadPool(THREAD_COUNT);

    private static String code = null;

    // private Document documentCookie = null, documetPageOfCode = null;

    public static void main(String[] args) {
        // отримуємо массик кукісів
        for (int i = 0; i < COOKIES_COUNT; i++)
            getCookie(i);

        // в THREAD_COUNT потоках будем відсилати запроси
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.schedule(new Runnable() {
                @Override
                public void run() {
                    // цикл до тих пір, поки код не отриманий
                    while (code == null || code.isEmpty()) {
                        try {
                            getAndSavePage();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }, 100, TimeUnit.MICROSECONDS);
        }

        executor.shutdown();
    }

    private static void getCookie(int i) {
        try {
            Document document = Jsoup.connect(URL)
                    .timeout(15000)
                    .get();

            if (document == null) {
                getCookie(i);
                return;
            }

            Elements scriptElements = document.getElementsByTag("script");

            Pattern p = Pattern.compile("(?is)document.cookie=\"(.+?);");
            Matcher m = p.matcher(scriptElements.html());

            String cookie = "";

            while (m.find()) {
                System.out.println("Cookie is " + m.group(1));
                cookie = m.group(1);
            }

            if (cookie.isEmpty()) {
                getCookie(i);
            } else {
                cookies.add(i, cookie);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String nextCookie() {
        String cookie = cookies.get(counter.getAndIncrement() % COOKIES_COUNT);
        if (cookie == null) return nextCookie();
        return cookie;
    }

    private static void getAndSavePage() {
        Document document = null;

        try {
            document = Jsoup.connect(URL)
                    .header(COOKIE_NAME, nextCookie())
                    .get();

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (document == null) return;

        // TODO save document to file. Зберігати отриманий html в файли.

        //System.out.println(document.toString());

        // TODO потрібно підправити вибірку коду.
        code = document.select("div.article_body").select("li").get(3).select("strong").first().html();

        System.out.println("CODE is '" + code + "'");


//                    Pattern p1 = Pattern.compile("(?is):\"(.+?)<");
//                    Matcher m2 = p1.matcher(elementContainsCode.html());
//
//                    while (m2.find()) {
//                        System.out.println("CODE" + m2.group(1)); // value only
//                    }

        // відсилаємо емайл якщо знайдений код. Потрібно забезпечити гарантію що тільки 1 раз відправиться код, щоб не заспамити листами.
        if (code != null && !code.isEmpty())
            sendEmail(code);
    }

    private static void sendEmail(String code) {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");

        Session session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("elvirafeltsan@gmail.com", "efkmAD78");
            }
        });

        try {
            final Message message = new MimeMessage(session);
            String passport = "passport.pdf";
            String contract = "contract.pdf";
            Multipart multipart = new MimeMultipart();
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            String m = "file attached. ";
            messageBodyPart.setText(m, "utf-8", "html");
            multipart.addBodyPart(messageBodyPart);

//            MimeBodyPart attachmentBodyPart = new MimeBodyPart();
//            attachmentBodyPart.attachFile(new File(path + "/" + passport));
//            attachmentBodyPart.attachFile(new File(path + "/" + contract));
//            multipart.addBodyPart(attachmentBodyPart);
            message.setFrom(new InternetAddress("elvirafeltsan@gmail.com"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("ivan.feltsan@gmail.com"));
            message.setSubject(code);
            message.setContent(multipart);

            Transport.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException(e);

//        } catch (IOException e) {
//            e.printStackTrace();
        }
    }
}
