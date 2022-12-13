package orm;

import orm.annotations.Column;
import orm.annotations.Entity;
import orm.annotations.Id;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EntityManager<E> implements DBContext<E> {

    private Connection connection;

    public EntityManager(Connection connection) {
        this.connection = connection;
    }


    @Override
    public boolean persist(E entity) throws SQLException, IllegalAccessException {
        String tableName = this.getTableName(entity.getClass());
        String fieldList = this.getDBFieldsWithoutID(entity);
        String valueList = this.getInsertValuesWithoutID(entity);

        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, fieldList, valueList);
        return this.connection.prepareStatement(sql).execute();
    }

    @Override
    public Iterable<E> find(Class<E> entityType) throws SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return find(entityType, null);
    }

    @Override
    public Iterable<E> find(Class<E> entityType, String where) throws SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        String tableName = this.getTableName(entityType);

        String sql = String.format("SELECT * FROM %s %s", tableName, where.equals("") ? "" : "WHERE " + where);

        ResultSet resultSet = this.connection.prepareStatement(sql).executeQuery();

        List<E> result = new ArrayList<>();

        E lastResult = this.createEntity(entityType, resultSet);
        while (lastResult != null) {
            result.add(lastResult);
            lastResult = createEntity(entityType, resultSet);
        }

        return result;

    }

    @Override
    public E findFirst(Class<E> entityType) throws SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return findFirst(entityType, "");
    }

    @Override
    public E findFirst(Class<E> entityType, String where) throws SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        String tableName = this.getTableName(entityType);

        String sql = String.format("SELECT * FROM %s %s LIMIT 1", tableName, where.equals("") ? "" : "WHERE " + where);

        ResultSet resultSet = this.connection.prepareStatement(sql).executeQuery();

        return this.createEntity(entityType, resultSet);

    }

    private E createEntity(Class<E> entityType, ResultSet resultSet) throws SQLException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (!resultSet.next()) {
            return null;
        }

        E entity = entityType.getDeclaredConstructor().newInstance();

        Field[] fields = entityType.getDeclaredFields();

        for (Field field : fields) {
            if (!field.isAnnotationPresent(Column.class) && !field.isAnnotationPresent(Id.class)) {
                continue;
            }

            Column columnAnnotation = field.getAnnotation(Column.class);

            String fieldName = columnAnnotation == null
                    ? field.getName()
                    : columnAnnotation.name();

            String value = resultSet.getString(fieldName);
            entity = fillData(entity, field, value);
        }

        return entity;

    }

    private E fillData(E entity, Field field, String value) throws IllegalAccessException {
        field.setAccessible(true);

        if (field.getType() == long.class || field.getType() == Long.class) {
            field.setLong(entity, Long.parseLong(value));
        } else if (field.getType() == int.class || field.getType() == Integer.class) {
            field.setInt(entity, Integer.parseInt(value));
        } else if (field.getType() == LocalDate.class) {
            field.set(entity, LocalDate.parse(value));
        } else if (field.getType() == String.class) {
            field.set(entity, value);
        } else {
            throw new ORMException("Unsupported type " + field.getType());
        }

        return entity;
    }


    private String getTableName(Class<?> clazz) {
        Entity annotation = clazz.getAnnotation(Entity.class);

        if (annotation == null) {
            throw new ORMException("Provided class does not have Entity annotation.");
        }

        return annotation.name();
    }


    private String getDBFieldsWithoutID(E entity) {
        Field[] fields = entity.getClass().getDeclaredFields();

        List<String> fieldsNames = new ArrayList<>();

        for (Field field : fields) {
            Column currentAnnotation = field.getAnnotation(Column.class);

            if (currentAnnotation != null) {
                fieldsNames.add(currentAnnotation.name());
            }
        }

        return String.join(", ", fieldsNames);
    }

    private String getInsertValuesWithoutID(E entity) throws IllegalAccessException {
        Field[] fields = entity.getClass().getDeclaredFields();

        List<String> result = new ArrayList<>();


        for (Field field : fields) {
            Column currentAnnotation = field.getAnnotation(Column.class);

            if (currentAnnotation == null) {
                continue;
            }

            field.setAccessible(true);
            Object value = field.get(entity);
            result.add("\"" + value.toString() + "\"");
        }

        return String.join(", ", result);

    }
}
