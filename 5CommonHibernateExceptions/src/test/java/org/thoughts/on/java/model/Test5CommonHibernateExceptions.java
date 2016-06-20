package org.thoughts.on.java.model;

import java.util.HashMap;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.OptimisticLockException;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;
import javax.persistence.RollbackException;

import org.apache.log4j.Logger;
import org.hibernate.LazyInitializationException;
import org.hibernate.PersistentObjectException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class Test5CommonHibernateExceptions {

	Logger log = Logger.getLogger(this.getClass().getName());

	private EntityManagerFactory emf;

	@Before
	public void init() {
		emf = Persistence.createEntityManagerFactory("my-persistence-unit");
	}

	@After
	public void close() {
		emf.close();
	}

	@Test(expected = LazyInitializationException.class)
	public void lazyInitializationException() {
		log.info("... lazyInitializationException ...");

		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();

		Author a = em.find(Author.class, 1L);

		em.getTransaction().commit();
		em.close();
		
		log.info(a.getFirstName() + " " + a.getLastName() + " wrote "+a.getBooks().size() + " books.");
	}
	
	@Test
	public void lazyInitializationExceptionFixed() {
		log.info("... lazyInitializationExceptionFixed ...");

		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();

		EntityGraph<?> graph = em.getEntityGraph("graph.AuthorBooks");
		HashMap<String, Object> properties = new HashMap<>();
		properties.put("javax.persistence.fetchgraph", graph);

		Author a = em.find(Author.class, 1L, properties);

		em.getTransaction().commit();
		em.close();
		
		log.info(a.getFirstName() + " " + a.getLastName() + " wrote "+a.getBooks().size() + " books.");
	}
	
	@Test
	public void optimisticLockException() {
		log.info("... optimisticLockException ...");

		// EntityManager and transaction 1
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		
		// EntityManager and transaction 2
		EntityManager em2 = emf.createEntityManager();
		em2.getTransaction().begin();

		// update 1
		Author a = em.find(Author.class, 1L);
		a.setFirstName("changed");
		
		// update 2
		Author a2 = em2.find(Author.class, 1L);
		a2.setFirstName("changed");

		// commit transaction 1
		em.getTransaction().commit();
		em.close();
		
		// commit transaction 2
		try {
			em2.getTransaction().commit();
			Assert.fail();
		} catch (RollbackException e) {
			Assert.assertTrue(e.getCause() instanceof OptimisticLockException);
			log.info("2nd transaction failed with an OptimisticLockException");
		}
		
		em2.close();
	}
	
	@Test
	public void annotationException() {
		log.info("... annotationException ...");

		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();

		Author a = new Author();
		a.setFirstName("first");
		a.setLastName("last");
		
		em.persist(a);

		em.getTransaction().commit();
		em.close();
	}
	
	@Test
	public void persistentObjectException1() {
		log.info("... persistentObjectException1 ...");

		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();

		Author a = new Author();
		a.setId(42L);
		a.setFirstName("first");
		a.setLastName("last");
		
		try {
			em.persist(a);
			Assert.fail();
		}catch (PersistenceException e) {
			Assert.assertTrue(e.getCause() instanceof PersistentObjectException);
			log.info("Persist failed with expected PersistentObjectException");
		}
		
		em.getTransaction().rollback();
		em.close();
	}
	
	@Test
	public void persistentObjectException2() {
		log.info("... persistentObjectException2 ...");

		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();

		Author a = em.find(Author.class, 1L);
		em.getTransaction().commit();
		em.close();
		
		EntityManager em2 = emf.createEntityManager();
		em2.getTransaction().begin();
		
		try {
			em2.persist(a);
			Assert.fail();
		}catch (PersistenceException e) {
			Assert.assertTrue(e.getCause() instanceof PersistentObjectException);
			log.info("Persist failed with expected PersistentObjectException");
		}

		em2.getTransaction().rollback();
		em2.close();
	}
}
