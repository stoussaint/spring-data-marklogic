package org.springframework.data.marklogic.datasource;

import com.marklogic.xcc.ContentSource;
import com.marklogic.xcc.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.marklogic.datasource.lookup.IsolationLevelContentSourceRouter;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.jta.JtaTransactionObject;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.transaction.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @since 17.10.2005
 */
public class DatabaseConnectorJtaTransactionTest {

	private Session session;
	private ContentSource contentSource;
	private UserTransaction userTransaction;
	private TransactionManager transactionManager;
	private Transaction transaction;

	@Before
	public void setup() throws Exception {
		session = mock(Session.class);
		contentSource = mock(ContentSource.class);
		userTransaction = mock(UserTransaction.class);
		transactionManager = mock(TransactionManager.class);
		transaction = mock(Transaction.class);
		given(contentSource.newSession()).willReturn(session);
	}

	@After
	public void verifyTransactionSynchronizationManagerState() {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertNull(TransactionSynchronizationManager.getCurrentTransactionName());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		assertNull(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
	}

	@Test
	public void testJtaTransactionCommit() throws Exception {
		doTestJtaTransaction(false);
	}

	@Test
	public void testJtaTransactionRollback() throws Exception {
		doTestJtaTransaction(true);
	}

	private void doTestJtaTransaction(final boolean rollback) throws Exception {
		if (rollback) {
			given(userTransaction.getStatus()).willReturn(
					Status.STATUS_NO_TRANSACTION,Status.STATUS_ACTIVE);
		}
		else {
			given(userTransaction.getStatus()).willReturn(
					Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);
		}

		JtaTransactionManager ptm = new JtaTransactionManager(userTransaction);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(contentSource));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(contentSource));
				assertTrue("JTA synchronizations active", TransactionSynchronizationManager.isSynchronizationActive());
				assertTrue("Is new transaction", status.isNewTransaction());

				Session c = ContentSourceUtils.getSession(contentSource);
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(contentSource));
				ContentSourceUtils.releaseSession(c, contentSource);

				c = ContentSourceUtils.getSession(contentSource);
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(contentSource));
				ContentSourceUtils.releaseSession(c, contentSource);

				if (rollback) {
					status.setRollbackOnly();
				}
			}
		});

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(contentSource));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		verify(userTransaction).begin();
		if (rollback) {
			verify(userTransaction).rollback();
		}
		verify(session).close();
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiresNew() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(false, false, false, false);
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiresNewWithAccessAfterResume() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(false, false, true, false);
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiresNewWithOpenOuterConnection() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(false, true, false, false);
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiresNewWithOpenOuterConnectionAccessed() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(false, true, true, false);
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiresNewWithTransactionAwareContentSource() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(false, false, true, true);
	}

	@Test
	public void testJtaTransactionRollbackWithPropagationRequiresNew() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(true, false, false, false);
	}

	@Test
	public void testJtaTransactionRollbackWithPropagationRequiresNewWithAccessAfterResume() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(true, false, true, false);
	}

	@Test
	public void testJtaTransactionRollbackWithPropagationRequiresNewWithOpenOuterConnection() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(true, true, false, false);
	}

	@Test
	public void testJtaTransactionRollbackWithPropagationRequiresNewWithOpenOuterConnectionAccessed() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(true, true, true, false);
	}

	@Test
	public void testJtaTransactionRollbackWithPropagationRequiresNewWithTransactionAwareContentSource() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(true, false, true, true);
	}

	private void doTestJtaTransactionWithPropagationRequiresNew(
			final boolean rollback, final boolean openOuterConnection, final boolean accessAfterResume,
			final boolean useTransactionAwareContentSource) throws Exception {

		given(transactionManager.suspend()).willReturn(transaction);
		if (rollback) {
			given(userTransaction.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
					Status.STATUS_ACTIVE);
		}
		else {
			given(userTransaction.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
					Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);
		}

		given(session.getTransactionMode()).willReturn(Session.TransactionMode.QUERY);

		final ContentSource dsToUse = useTransactionAwareContentSource ?
				new TransactionAwareContentSourceProxy(contentSource) : contentSource;

		JtaTransactionManager ptm = new JtaTransactionManager(userTransaction, transactionManager);
		final TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(dsToUse));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(dsToUse));
				assertTrue("JTA synchronizations active", TransactionSynchronizationManager.isSynchronizationActive());
				assertTrue("Is new transaction", status.isNewTransaction());

				Session s = ContentSourceUtils.getSession(dsToUse);
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(dsToUse));
				s.getTransactionMode();
				ContentSourceUtils.releaseSession(s, dsToUse);

				s = ContentSourceUtils.getSession(dsToUse);
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(dsToUse));
				if (!openOuterConnection) {
					ContentSourceUtils.releaseSession(s, dsToUse);
				}


				for (int i = 0; i < 5; i++) {

					tt.execute(new TransactionCallbackWithoutResult() {
						@Override
						protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
							assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(dsToUse));
							assertTrue("JTA synchronizations active", TransactionSynchronizationManager.isSynchronizationActive());
							assertTrue("Is new transaction", status.isNewTransaction());
							
							Session s = ContentSourceUtils.getSession(dsToUse);
							s.getTransactionMode();
							assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(dsToUse));
							ContentSourceUtils.releaseSession(s, dsToUse);

							s = ContentSourceUtils.getSession(dsToUse);
							assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(dsToUse));
							ContentSourceUtils.releaseSession(s, dsToUse);
						}
					});

				}

				if (rollback) {
					status.setRollbackOnly();
				}

				if (accessAfterResume) {
					if (!openOuterConnection) {
						s = ContentSourceUtils.getSession(dsToUse);
					}
					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(dsToUse));
					s.getTransactionMode();
					ContentSourceUtils.releaseSession(s, dsToUse);

					s = ContentSourceUtils.getSession(dsToUse);
					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(dsToUse));
					ContentSourceUtils.releaseSession(s, dsToUse);
				}

				else {
					if (openOuterConnection) {
						ContentSourceUtils.releaseSession(s, dsToUse);
					}
				}
			}
		});

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(dsToUse));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		verify(userTransaction, times(6)).begin();
		verify(transactionManager, times(5)).resume(transaction);
		if(rollback) {
			verify(userTransaction, times(5)).commit();
			verify(userTransaction).rollback();
		} else {
			verify(userTransaction, times(6)).commit();
		}
		if(accessAfterResume && !openOuterConnection) {
			verify(session, times(7)).close();
		}
		else {
			verify(session, times(6)).close();
		}
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiredWithinSupports() throws Exception {
		doTestJtaTransactionCommitWithNewTransactionWithinEmptyTransaction(false, false);
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiredWithinNotSupported() throws Exception {
		doTestJtaTransactionCommitWithNewTransactionWithinEmptyTransaction(false, true);
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiresNewWithinSupports() throws Exception {
		doTestJtaTransactionCommitWithNewTransactionWithinEmptyTransaction(true, false);
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiresNewWithinNotSupported() throws Exception {
		doTestJtaTransactionCommitWithNewTransactionWithinEmptyTransaction(true, true);
	}

	private void doTestJtaTransactionCommitWithNewTransactionWithinEmptyTransaction(
			final boolean requiresNew, boolean notSupported) throws Exception {

		if (notSupported) {
			given(userTransaction.getStatus()).willReturn(
					Status.STATUS_ACTIVE,
					Status.STATUS_NO_TRANSACTION,
					Status.STATUS_ACTIVE,
					Status.STATUS_ACTIVE);
			given(transactionManager.suspend()).willReturn(transaction);
		}
		else {
			given(userTransaction.getStatus()).willReturn(
					Status.STATUS_NO_TRANSACTION,
					Status.STATUS_NO_TRANSACTION,
					Status.STATUS_ACTIVE,
					Status.STATUS_ACTIVE);
		}

		final ContentSource contentSource = mock(ContentSource.class);
		final Session session1 = mock(Session.class);
		final Session session2 = mock(Session.class);
		given(contentSource.newSession()).willReturn(session1, session2);

		final JtaTransactionManager ptm = new JtaTransactionManager(userTransaction, transactionManager);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(notSupported ?
				TransactionDefinition.PROPAGATION_NOT_SUPPORTED : TransactionDefinition.PROPAGATION_SUPPORTS);

		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
				assertSame(session1, ContentSourceUtils.getSession(contentSource));
				assertSame(session1, ContentSourceUtils.getSession(contentSource));

				TransactionTemplate tt2 = new TransactionTemplate(ptm);
				tt2.setPropagationBehavior(requiresNew ?
						TransactionDefinition.PROPAGATION_REQUIRES_NEW : TransactionDefinition.PROPAGATION_REQUIRED);
				tt2.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) {
						assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
						assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
						assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
						assertSame(session2, ContentSourceUtils.getSession(contentSource));
						assertSame(session2, ContentSourceUtils.getSession(contentSource));
					}
				});

				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
				assertSame(session1, ContentSourceUtils.getSession(contentSource));
			}
		});
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		verify(userTransaction).begin();
		verify(userTransaction).commit();
		if (notSupported) {
			verify(transactionManager).resume(transaction);
		}
		verify(session2).close();
		verify(session1).close();
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiresNewAndSuspendException() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNewAndBeginException(true, false, false);
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiresNewWithOpenOuterConnectionAndSuspendException() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNewAndBeginException(true, true, false);
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiresNewWithTransactionAwareContentSourceAndSuspendException() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNewAndBeginException(true, false, true);
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiresNewWithOpenOuterConnectionAndTransactionAwareContentSourceAndSuspendException() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNewAndBeginException(true, true, true);
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiresNewAndBeginException() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNewAndBeginException(false, false, false);
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiresNewWithOpenOuterConnectionAndBeginException() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNewAndBeginException(false, true, false);
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiresNewWithOpenOuterConnectionAndTransactionAwareContentSourceAndBeginException() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNewAndBeginException(false, true, true);
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiresNewWithTransactionAwareContentSourceAndBeginException() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNewAndBeginException(false, false, true);
	}

	private void doTestJtaTransactionWithPropagationRequiresNewAndBeginException(boolean suspendException,
			final boolean openOuterConnection, final boolean useTransactionAwareContentSource) throws Exception {

		given(userTransaction.getStatus()).willReturn(
				Status.STATUS_NO_TRANSACTION,
				Status.STATUS_ACTIVE,
				Status.STATUS_ACTIVE);
		if (suspendException) {
			given(transactionManager.suspend()).willThrow(new SystemException());
		}
		else {
			given(transactionManager.suspend()).willReturn(transaction);
			willThrow(new SystemException()).given(userTransaction).begin();
		}

		given(session.getTransactionMode()).willReturn(Session.TransactionMode.QUERY);

		final ContentSource dsToUse = useTransactionAwareContentSource ?
				new TransactionAwareContentSourceProxy(contentSource) : contentSource;
		if (dsToUse instanceof TransactionAwareContentSourceProxy) {
			((TransactionAwareContentSourceProxy) dsToUse).setReobtainTransactionalSessions(true);
		}

		JtaTransactionManager ptm = new JtaTransactionManager(userTransaction, transactionManager);
		final TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(dsToUse));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
					assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(dsToUse));
					assertTrue("JTA synchronizations active", TransactionSynchronizationManager.isSynchronizationActive());
					assertTrue("Is new transaction", status.isNewTransaction());

					Session c = ContentSourceUtils.getSession(dsToUse);
					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(dsToUse));
					c.getTransactionMode();
					ContentSourceUtils.releaseSession(c, dsToUse);

					c = ContentSourceUtils.getSession(dsToUse);
					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(dsToUse));
					if (!openOuterConnection) {
						ContentSourceUtils.releaseSession(c, dsToUse);
					}


					try {
						tt.execute(new TransactionCallbackWithoutResult() {
							@Override
							protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
								assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(dsToUse));
								assertTrue("JTA synchronizations active", TransactionSynchronizationManager.isSynchronizationActive());
								assertTrue("Is new transaction", status.isNewTransaction());

								Session c = ContentSourceUtils.getSession(dsToUse);
								assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(dsToUse));
								ContentSourceUtils.releaseSession(c, dsToUse);

								c = ContentSourceUtils.getSession(dsToUse);
								assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(dsToUse));
								ContentSourceUtils.releaseSession(c, dsToUse);
							}
						});
					}
					finally {
						if (openOuterConnection) {
							c.getTransactionMode();
							ContentSourceUtils.releaseSession(c, dsToUse);

						}
					}
				}
			});

			fail("Should have thrown TransactionException");
		}
		catch (TransactionException ex) {
			// expected
		}

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(dsToUse));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		verify(userTransaction).begin();
		if(suspendException) {
			verify(userTransaction).rollback();
		}

		if (suspendException) {
			verify(session, atLeastOnce()).close();
		}
		else {
			verify(session, never()).close();
		}
	}

	@Test
	public void testJtaTransactionWithConnectionHolderStillBound() throws Exception {
		@SuppressWarnings("serial")
		JtaTransactionManager ptm = new JtaTransactionManager(userTransaction) {

			@Override
			protected void doRegisterAfterCompletionWithJtaTransaction(
					JtaTransactionObject txObject,
					final List<TransactionSynchronization> synchronizations)
					throws RollbackException, SystemException {
				Thread async = new Thread() {
					@Override
					public void run() {
						invokeAfterCompletion(synchronizations, TransactionSynchronization.STATUS_COMMITTED);
					}
				};
				async.start();
				try {
					async.join();
				}
				catch (InterruptedException ex) {
					ex.printStackTrace();
				}
			}
		};
		TransactionTemplate tt = new TransactionTemplate(ptm);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(contentSource));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		given(userTransaction.getStatus()).willReturn(Status.STATUS_ACTIVE);
		for (int i = 0; i < 3; i++) {
			final boolean releaseCon = (i != 1);

			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
					assertTrue("JTA synchronizations active", TransactionSynchronizationManager.isSynchronizationActive());
					assertTrue("Is existing transaction", !status.isNewTransaction());

					Session c = ContentSourceUtils.getSession(contentSource);
					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(contentSource));
					ContentSourceUtils.releaseSession(c, contentSource);

					c = ContentSourceUtils.getSession(contentSource);
					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(contentSource));
					if (releaseCon) {
						ContentSourceUtils.releaseSession(c, contentSource);
					}
				}
			});

			if (!releaseCon) {
				assertTrue("Still has session holder", TransactionSynchronizationManager.hasResource(contentSource));
			}
			else {
				assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(contentSource));
			}
			assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		}
		verify(session, times(3)).close();
	}

	@Test
	public void testJtaTransactionWithIsolationLevelContentSourceAdapter() throws Exception {
		given(userTransaction.getStatus()).willReturn(
				Status.STATUS_NO_TRANSACTION,
				Status.STATUS_ACTIVE,
				Status.STATUS_ACTIVE,
				Status.STATUS_NO_TRANSACTION,
				Status.STATUS_ACTIVE,
				Status.STATUS_ACTIVE);

		final IsolationLevelContentSourceAdapter dsToUse = new IsolationLevelContentSourceAdapter();
		dsToUse.setTargetContentSource(contentSource);
		dsToUse.afterPropertiesSet();

		JtaTransactionManager ptm = new JtaTransactionManager(userTransaction);
		ptm.setAllowCustomIsolationLevels(true);

		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				Session c = ContentSourceUtils.getSession(dsToUse);
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(dsToUse));
				assertSame(session, c);
				ContentSourceUtils.releaseSession(c, dsToUse);
			}
		});

		tt.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		tt.setReadOnly(true);
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				Session c = ContentSourceUtils.getSession(dsToUse);
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(dsToUse));
				assertSame(session, c);
				ContentSourceUtils.releaseSession(c, dsToUse);
			}
		});

		verify(userTransaction, times(2)).begin();
		verify(userTransaction, times(2)).commit();
		verify(session).setTransactionMode(Session.TransactionMode.QUERY);
		verify(session, times(2)).close();
	}

	@Test
	public void testJtaTransactionWithIsolationLevelContentSourceRouter() throws Exception {
		doTestJtaTransactionWithIsolationLevelContentSourceRouter();
	}

	private void doTestJtaTransactionWithIsolationLevelContentSourceRouter() throws Exception {
		given(userTransaction.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE, Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);

		final ContentSource contentSource1 = mock(ContentSource.class);
		final Session session1 = mock(Session.class);
		given(contentSource1.newSession()).willReturn(session1);

		final ContentSource contentSource2 = mock(ContentSource.class);
		final Session session2 = mock(Session.class);
		given(contentSource2.newSession()).willReturn(session2);

		final IsolationLevelContentSourceRouter dsToUse = new IsolationLevelContentSourceRouter();
		Map<Object, Object> targetContentSources = new HashMap<Object, Object>();

		targetContentSources.put("ISOLATION_REPEATABLE_READ", contentSource2);
		dsToUse.setDefaultTargetContentSource(contentSource1);

		dsToUse.setTargetContentSources(targetContentSources);
		dsToUse.afterPropertiesSet();

		JtaTransactionManager ptm = new JtaTransactionManager(userTransaction);
		ptm.setAllowCustomIsolationLevels(true);

		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				Session c = ContentSourceUtils.getSession(dsToUse);
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(dsToUse));
				assertSame(session1, c);
				ContentSourceUtils.releaseSession(c, dsToUse);
			}
		});

		tt.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				Session c = ContentSourceUtils.getSession(dsToUse);
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(dsToUse));
				assertSame(session2, c);
				ContentSourceUtils.releaseSession(c, dsToUse);
			}
		});

		verify(userTransaction, times(2)).begin();
		verify(userTransaction, times(2)).commit();
		verify(session1).close();
		verify(session2).close();
	}
}
