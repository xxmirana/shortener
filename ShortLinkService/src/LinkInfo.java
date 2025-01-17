import java.util.*;
import java.util.concurrent.*;
import java.time.*;

class LinkInfo {

    public static void createShortLink(Scanner scanner, User user, Map<String, Link> links, ScheduledExecutorService scheduler, Properties config) {
        try {
            System.out.println("Введите URL для создания короткой ссылки:");
            String originalUrl = scanner.nextLine();

            System.out.println("Задайте время жизни короткой ссылки в секундах (от 1 до 3600):");
            int userLifetime = Integer.parseInt(scanner.nextLine());
            if (userLifetime <= 0) throw new IllegalArgumentException("Время жизни должно быть больше 0");

            int defaultLifetime = Integer.parseInt(config.getProperty("defaultLifetimeSeconds"));
            int lifetime = Math.min(userLifetime, defaultLifetime);

            System.out.println("Задайте лимит переходов по короткой ссылке:");
            int userClickLimit = Integer.parseInt(scanner.nextLine());
            if (userClickLimit <= 0) throw new IllegalArgumentException("Лимит переходов должен быть больше 0");

            int defaultClickLimit = Integer.parseInt(config.getProperty("defaultClickLimit"));
            int clickLimit = Math.min(userClickLimit, defaultClickLimit);

            String shortUrl = generateShortUrl(links);
            Instant expiry = Instant.now().plusSeconds(lifetime);

            Link link = new Link(originalUrl, shortUrl, user.getUuid(), expiry, clickLimit);
            links.put(shortUrl, link);

            System.out.println("Ваша короткая ссылка: " + shortUrl);

            scheduler.schedule(() -> {
                links.remove(shortUrl);
                System.out.println("Срок действия вашей короткой ссылки " + shortUrl + " истёк, ссылка была удалена.");
            }, lifetime, TimeUnit.SECONDS);

        } catch (NumberFormatException e) {
            System.out.println("Ошибка ввода: Нечисловое значение " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка ввода: " + e.getMessage());
        }

    }


    public static void viewLinks(Scanner scanner, User user, Map<String, Link> links) {
        System.out.println("Ваши ссылки:");

        for (Link link : links.values()) {
            if (link.getOwnerUuid().equals(user.getUuid())) {
                System.out.println(link.getShortUrl() + " преобразовано в " + link.getOriginalUrl() + " (Клики: " + link.getClicks() + "/" + link.getClickLimit() + ")");
            }
        }
    }

    public static void updateClickLimit(Scanner scanner, User user, Map<String, Link> links) {
        System.out.println("Введите короткую ссылку для изменения лимита переходов:");
        String shortUrl = scanner.nextLine();

        Link link = links.get(shortUrl);
        if (link == null || !link.getOwnerUuid().equals(user.getUuid())) {
            System.out.println("Ссылка не найдена");
            return;
        }

        System.out.println("Введите новый лимит переходов для короткой ссылки:");
        int newLimit = Integer.parseInt(scanner.nextLine());
        link.setClickLimit(newLimit);

        System.out.println("Лимит переходов успешно обновлён");
    }

    public static void deleteLink(Scanner scanner, User user, Map<String, Link> links) {
        System.out.println("Введите короткую ссылку для удаления:");
        String shortUrl = scanner.nextLine();

        Link link = links.get(shortUrl);
        if (link == null || !link.getOwnerUuid().equals(user.getUuid())) {
            System.out.println("Ссылка не найдена");
            return;
        }

        links.remove(shortUrl);
        System.out.println("Ссылка успешно удалена");
    }

    private static String generateShortUrl(Map<String, Link> links) {
        String shortUrl;
        do {
            shortUrl = "clck.ru" + UUID.randomUUID().toString().substring(0, 8);
        } while (links.containsKey(shortUrl));
        return shortUrl;
    }

}