/*
 * Copyright 2013- Yan Bonnel
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.ybonnel.simpleweb4j.model;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.reflections.Reflections;

import javax.persistence.Entity;
import java.io.Serializable;
import java.util.Collection;

/**
 * Simple entityManager from SimpleWeb4j.
 * Use :
 * {@code SimpleEntityManager<Computer, Long> simpleEntityManager = new SimpleEntityManager<>(Computer.class);
 * }
 * @param <T> The entity to manage.
 * @param <I> The id type.
 */
public class SimpleEntityManager<T, I extends Serializable> {

    /**
     * Class of the entity.
     */
    private Class<T> entityClass;

    /**
     * Constructor.
     * @param entityClass class of entity.
     */
    public SimpleEntityManager(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    /**
     * Path to hibernate config file.
     */
    private static String cfgPath = "fr/ybonnel/simpleweb4j/model/hibernate.cfg.xml";

    /**
     * Change the path to hibernate config file.
     * @param newCfgPath new path to hibernate config file.
     */
    public static void setCfgPath(String newCfgPath) {
        cfgPath = newCfgPath;
    }

    /**
     * Package of entities.
     */
    private static String entitiesPackage = null;

    /**
     * Change the package of entities.
     * @param newEntitiesPackge new package of entities.
     */
    public static void setEntitiesPackage(String newEntitiesPackge) {
        entitiesPackage = newEntitiesPackge;
    }

    /**
     * The sesssionFactory.
     */
    private static SessionFactory sessionFactory = null;

    /**
     * Method used to know if the current application have entities to manage.
     * @return true is there's entities to manage.
     */
    public static boolean hasEntities() {
        return !getAnnotatedClasses().isEmpty();
    }

    /**
     * Get the session factory.
     * @return session factory.
     */
    private static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            synchronized (SimpleEntityManager.class) {
                if (sessionFactory == null) {

                    Configuration configuration = new Configuration();
                    configuration.configure(cfgPath);
                    for (Class<?> entityClass : getAnnotatedClasses()) {
                        configuration.addAnnotatedClass(entityClass);
                    }
                    ServiceRegistry serviceRegistry = new ServiceRegistryBuilder().applySettings(
                            configuration.getProperties()).buildServiceRegistry();
                    sessionFactory = configuration.buildSessionFactory(serviceRegistry);
                }
            }
        }
        return sessionFactory;
    }

    /**
     * Opened sessions.
     */
    private static ThreadLocal<Session> currentSessions = new ThreadLocal<>();

    /**
     * Open a session (don't use it directly, the session is automatically open by SimpleWeb4j).
     * @return the opened session.
     */
    public static Session openSession() {
        if (getCurrentSession() != null)  {
            throw new IllegalStateException("There is already a session for the current thread");
        }
        Session session = getSessionFactory().openSession();
        currentSessions.set(session);
        return session;
    }

    /**
     * Close the current session (don't use it directly, the session is automatically close by SimpleWeb4j).
     */
    public static void closeSession() {
        Session session = currentSessions.get();
        if (session == null) {
            throw new IllegalStateException("There is no session for the current thread");
        }
        session.close();
        currentSessions.remove();
    }

    /**
     * Get the current session.
     * @return the current session.
     */
    public static Session getCurrentSession() {
        return currentSessions.get();
    }

    /**
     * Annotated classes (list of all entities).
     */
    private static Collection<Class<?>> annotatedClasses = null;
    /**
     * Last package use for entities.
     */
    private static String actualEntitiesPackage = null;

    /**
     * Use to know is entities package has changed (used for unit tests).
     * @return true is entities packages has changed.
     */
    protected static boolean actualEntitiesPackageHasChange() {
        if (actualEntitiesPackage == null) {
            return entitiesPackage != null;
        }
        return !actualEntitiesPackage.equals(entitiesPackage);
    }

    /**
     * Get the annotated classes (entities).
     *
     * @return the list of annotated classes.
     */
    private static Collection<Class<?>> getAnnotatedClasses() {
        if (annotatedClasses == null || actualEntitiesPackageHasChange()) {
            Reflections reflections = entitiesPackage == null ? new Reflections("") : new Reflections(entitiesPackage);
            annotatedClasses = reflections.getTypesAnnotatedWith(Entity.class);
            actualEntitiesPackage = entitiesPackage;
        }
        return annotatedClasses;
    }

    /**
     * Save an entity.
     * @param entity the entity to save.
     */
    public void save(T entity) {
        getCurrentSession().save(entity);
    }

    /**
     * Update an entity.
     * @param entity entity to update.
     */
    public void update(T entity) {
        getCurrentSession().update(entity);
    }

    /**
     * Get an entity by the id.
     * @param id id of the entity.
     * @return the entity if found, null otherwise.
     */
    @SuppressWarnings("unchecked")
    public T getById(I id) {
        return (T) getCurrentSession().get(entityClass, id);
    }

    /**
     * Get all entities.
     * @return the list of all entities.
     */
    @SuppressWarnings("unchecked")
    public Collection<T> getAll() {
        return getCurrentSession().createCriteria(entityClass).list();
    }

    /**
     * Delete an entity.
     * @param id id of the entity.
     */
    public void delete(I id) {
        getCurrentSession().delete(getCurrentSession().get(entityClass, id));
    }
}
