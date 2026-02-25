package tn.esprit.pidev.dao;

import java.util.List;

/**
 * Generic DAO interface for CRUD operations. PIDEV - AgriRent.
 *
 * @param <T> entity type
 */
public interface IDao<T> {

    /**
     * Persists a new entity.
     *
     * @param entity the entity to create
     */
    void create(T entity);

    /**
     * Finds entity by id.
     *
     * @param id primary key
     * @return entity or null
     */
    T getById(int id);

    /**
     * Returns all entities.
     *
     * @return list of entities
     */
    List<T> getAll();

    /**
     * Updates an existing entity.
     *
     * @param entity the entity to update
     */
    void update(T entity);

    /**
     * Deletes entity by id.
     *
     * @param id primary key
     */
    void delete(int id);
}
