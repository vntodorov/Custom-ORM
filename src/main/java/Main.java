import entities.User;
import orm.Connector;
import orm.EntityManager;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws SQLException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        Scanner scanner = new Scanner(System.in);

        Connector.createConnection("root", "12345", "soft_uni");
        Connection connection = Connector.getConnection();

        EntityManager<User> userManager = new EntityManager<>(connection);

        User user = new User ("Second", 28, LocalDate.now());

        userManager.persist(user);

        User first = userManager.findFirst(User.class);

        System.out.println(first.getId() + " " + first.getUsername());

        userManager.find(User.class, "age > 18 AND registration_date > '2022-06-06'")
                .forEach( u -> System.out.println(u.toString()));
    }
}
