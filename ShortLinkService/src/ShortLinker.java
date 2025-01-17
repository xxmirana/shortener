import java.time.Instant;
import java.awt.Desktop;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

public class ShortLinker {

    private static final String SHORT_LINK_PREFIX = "clck.ru/";
    private static final String CONFIG_FILE = "config.properties";
    private static final String DATA_FILE = "data.txt";

    private static Map<String, User> users = new HashMap<>();
    private static Map<String, Link> links = new ConcurrentHashMap<>();
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static Properties config = new Properties();

    public static void main(String[] args) throws IOException {
        loadConfig();
        loadData();
        runApp();
    }

    private static void runApp() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("1. Регистрация\n2. Вход\n3. Сокращение ссылки\n4. Выход");
            int choice = Integer.parseInt(scanner.nextLine());

            switch (choice) {
                case 1 -> registerUser(scanner);
                case 2 -> loginUser(scanner);
                case 3 -> processShortLink(scanner);
                case 4 -> {
                    saveData();
                    scheduler.shutdown();
                    System.exit(0);
                }
                default -> System.out.println("Ошибка: попробуйте снова");
            }
        }
    }

    private static void processShortLink(Scanner scanner) {
        System.out.println("Введите короткую ссылку:");
        String shortUrl = scanner.nextLine();

        Link link = links.get(shortUrl);
        if (link == null) {
            System.out.println("Ссылка некорректна и/или истек срок действия введенной ссылки");
            return;
        }

        if (link.getClicks() >= link.getClickLimit()) {
            System.out.println("Ссылка больше недоступна: Лимит переходов по ссылке исчерпан");
            return;
        }

        if (link.getExpiry().isBefore(Instant.now())) {
            System.out.println("Ссылка больше недоступна: Срок действия введенной ссылки истёк");
            links.remove(shortUrl);
            return;
        }

        link.incrementClicks();

        String originalUrl = link.getOriginalUrl();
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(originalUrl));
                System.out.println("Переход по ссылке на сайт в браузере: " + originalUrl);
            } catch (Exception e) {
                System.out.println("Ошибка при открытии короткой ссылки в браузере: " + e.getMessage());
            }
        } else {
            System.out.println("Вашим устройством не поддерживается автоматическое открытие ссылок в браузере");
            System.out.println("Ссылка: " + originalUrl);
        }
    }

    private static void registerUser(Scanner scanner) {
        System.out.println("Введите ваше имя для генерации идентификатора:");
        String name = scanner.nextLine();
        String uuid = UUID.randomUUID().toString();
        users.put(uuid, new User(uuid, name));
        System.out.println("Ваш идентификатор: " + uuid);
    }

    private static void loginUser(Scanner scanner) {
        System.out.println("Введите ваш идентификатор:");
        String uuid = scanner.nextLine();

        if (!users.containsKey(uuid)) {
            System.out.println("Ошибка: неверный идентификатор");
            return;
        }

        User user = users.get(uuid);
        System.out.println("Добро пожаловать, " + user.getName() + "!");

        while (true) {
            System.out.println("1. Создать короткую ссылку\n2. Мои ссылки\n3. Изменить лимит переходов по моим коротким ссылкам\n4. Удалить короткую ссылку\n5. Выйти");
            int choice = Integer.parseInt(scanner.nextLine());

            switch (choice) {
                case 1 -> LinkInfo.createShortLink(scanner, user, links, scheduler, config);
                case 2 -> LinkInfo.viewLinks(scanner, user, links);
                case 3 -> LinkInfo.updateClickLimit(scanner, user, links);
                case 4 -> LinkInfo.deleteLink(scanner, user, links);
                case 5 -> {
                    System.out.println("Вы вышли из системы. Ждем вас снова!");
                    return;
                }
                default -> System.out.println("Ошибка: некорректный выбор");
            }
        }
    }

    private static void saveConfig() throws IOException {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            config.store(fos, "Default Configuration");
        }
    }

    private static void loadConfig() throws IOException {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                config.load(fis);
            }
        } else {
            config.setProperty("defaultLifetimeSeconds", "3600");
            config.setProperty("defaultClickLimit", "10");
            saveConfig();
        }
    }

    private static void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(users);
            oos.writeObject(links);
        } catch (IOException e) {
            System.out.println("Ошибка сохранения данных: " + e.getMessage());
        }
    }

    private static void loadData() {
        File dataFile = new File(DATA_FILE);
        if (!dataFile.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(dataFile))) {
            users = (Map<String, User>) ois.readObject();
            links = (Map<String, Link>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Ошибка загрузки данных: " + e.getMessage());
        }
    }

}
