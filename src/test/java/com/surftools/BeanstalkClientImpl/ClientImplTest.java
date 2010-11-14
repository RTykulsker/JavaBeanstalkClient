package com.surftools.BeanstalkClientImpl;

/*

Copyright 2009-2010 Robert Tykulsker 

This file is part of JavaBeanstalkCLient.

JavaBeanstalkCLient is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version, or alternatively, the BSD license supplied
with this project in the file "BSD-LICENSE".

JavaBeanstalkCLient is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with JavaBeanstalkCLient.  If not, see <http://www.gnu.org/licenses/>.

*/

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.surftools.BeanstalkClient.BeanstalkException;
import com.surftools.BeanstalkClient.Client;
import com.surftools.BeanstalkClient.Job;

public class ClientImplTest extends TestCase {

	private String TEST_HOST = "localhost";
	private int TEST_PORT = 11300;

	
	public ClientImplTest(String testName) {
		super(testName);
	}

	
	public static Test suite() {
		return new TestSuite(ClientImplTest.class);
	}

	
	// ****************************************************************
	// Support methods
	// ****************************************************************

	/**
	 * ignore all currently watched tubes, retuned in ret[0] watch a new tube,
	 * returned in ret[1]
	 */
	Object[] pushWatchedTubes(Client client) {
		Object[] tubeNames = new Object[2];
		List<String> list = client.listTubesWatched();
		
		String newTubeName = "tube-" + UUID.randomUUID().toString();
		client.watch(newTubeName);

		for (String existingTubeName : list) {
			client.ignore(existingTubeName);
		}

		tubeNames[0] = list;
		tubeNames[1] = newTubeName;

		return tubeNames;
	}

	@SuppressWarnings("unchecked")
	void popWatchedTubes(Client client, Object[] tubeNames) {
		for (String tubeName : (List<String>) tubeNames[0]) {
			client.watch(tubeName);
		}

		client.ignore((String) tubeNames[1]);
	}
	
	private boolean serverSupportsUnderscoreInTubeName(Client client) {
		assertNotNull(client);
		
		String serverVersion = client.getServerVersion();
		assertNotNull(serverVersion);
		String[] tokens = serverVersion.split("\\.");
		assertEquals(3, tokens.length);
		
		int majorVersion = Integer.parseInt(tokens[0]);
		int minorVersion = Integer.parseInt(tokens[1]);
		int dotVersion = Integer.parseInt(tokens[2]);
		
		if (majorVersion >= 1 && minorVersion >= 4 && dotVersion >= 4) {
			return true;
		}
		
		return false;
	}

	// ****************************************************************
	// Producer methods
	// ****************************************************************

	public void testGetServerVersion() {
		Client client = new ClientImpl(TEST_HOST, TEST_PORT);
		
		String serverVersion = client.getServerVersion();
		assertNotNull(serverVersion);
		String[] tokens = serverVersion.split("\\.");
		assertEquals(3, tokens.length);
		
		int majorVersion = Integer.parseInt(tokens[0]);
		int minorVersion = Integer.parseInt(tokens[1]);
		int dotVersion = Integer.parseInt(tokens[2]);
		assertTrue(majorVersion >= 1);
		assertTrue(minorVersion >= 4);
		assertTrue(dotVersion >= 4);		
	}

	
	public void testBinaryData() {

		for (boolean useBlockIO : new boolean[] { false, true }) {
			Client client = new ClientImpl(TEST_HOST, TEST_PORT, useBlockIO);

			Object[] tubeNames = pushWatchedTubes(client);

			byte[] srcBytes = new byte[256];
			for (int i = 0; i < srcBytes.length; ++i) {
				srcBytes[i] = (byte) i;
			}

			// producer
			client.useTube((String) tubeNames[1]);
			long jobId = client.put(65536, 0, 120, srcBytes);
			assertTrue(jobId > 0);

			// consumer
			Job job = client.reserve(null);
			assertNotNull(job);
			long newJobId = job.getJobId();
			assertEquals(jobId, newJobId);

			// verify bytes
			byte[] dstBytes = job.getData();
			assertEquals(srcBytes.length, dstBytes.length);
			for (int i = 0; i < srcBytes.length; ++i) {
				assertEquals(srcBytes[i], dstBytes[i]);
			}

			client.delete(job.getJobId());

			popWatchedTubes(client, tubeNames);
		}
	}

	
	public void testUseTube() {
		Client client = new ClientImpl(TEST_HOST, TEST_PORT);
		client.useTube("foobar");
		
		// hashes are not valid in tube names
		try {
			client.useTube("foobar#");
			fail("no BAD_FORMAT thrown");
		} catch (BeanstalkException be) {
			assertEquals("BAD_FORMAT", be.getMessage());
		} catch (Exception e) {
			fail(e.getMessage());
		}

		// underscores are valid in tube names >= beanstalk 1.4.4.
		if (serverSupportsUnderscoreInTubeName(client)) {
			try {
				client.useTube("foobar_");
			} catch (Exception e) {
				fail(e.getMessage());
			}
		}
		
		// per pashields http://github.com/pashields/JavaBeanstalkClient.git
		// Names cannot start with hyphen
		try {
			client.useTube("-foobar");
			fail("no BAD_FORMAT thrown");
		} catch (BeanstalkException be) {
			assertEquals("BAD_FORMAT", be.getMessage());
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	
	public void testPut() {

		Client client = new ClientImpl(TEST_HOST, TEST_PORT);
		long jobId = client.put(65536, 0, 120, "testPut".getBytes());
		assertTrue(jobId > 0);
		client.delete(jobId);

		// invalid priority
		try {
			jobId = client
					.put(-1, 0, 120, "testPutNegativePriority".getBytes());
			client.delete(jobId);
			fail("no BAD_FORMAT thrown");
		} catch (BeanstalkException be) {
			assertEquals("BAD_FORMAT", be.getMessage());
		} catch (Exception e) {
			fail(e.getMessage());
		}

		// invalid priority
		try {
			jobId = client.put(Long.MAX_VALUE, 0, 120, "testPutHugePriority"
					.getBytes());
			client.delete(jobId);
			fail("no UNKNOWN_COMMAND thrown");
		} catch (BeanstalkException be) {
			assertEquals("invalid priority", be.getMessage());
		} catch (Exception e) {
			fail(e.getMessage());
		}

		// null data
		try {
			jobId = client.put(65536, 0, 120, null);
			client.delete(jobId);
			fail("no exception");
		} catch (BeanstalkException be) {
			assertEquals("null data", be.getMessage());
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	// ****************************************************************
	// Consumer methods
	// job-related
	// ****************************************************************

	public void testReserve() {

		Client client = new ClientImpl(TEST_HOST, TEST_PORT);

		Object[] tubeNames = pushWatchedTubes(client);
		
		String srcString = "testReserve";
		
		// producer
		client.useTube((String) tubeNames[1]);
		long jobId = client.put(65536, 0, 120, srcString.getBytes());
		assertTrue(jobId > 0);

		// consumer
		Job job = client.reserve(null);
		assertNotNull(job);
		long newJobId = job.getJobId();
		assertEquals(jobId, newJobId);

		String dstString = new String(job.getData());
		assertEquals(srcString, dstString);

		client.delete(job.getJobId());

		popWatchedTubes(client, tubeNames);
	}

	
	public void testReserveWithTimeout() {

		Client client = new ClientImpl(TEST_HOST, TEST_PORT);

		Object[] tubeNames = pushWatchedTubes(client);

		String srcString = "testReserveWithTimeout";

		int timeoutSeconds = 2;

		// producer
		client.useTube((String) tubeNames[1]);
		long putMillis = System.currentTimeMillis();
		long jobId = client.put(65536, timeoutSeconds, 120, srcString
				.getBytes());
		assertTrue(jobId > 0);

		// consumer
		Job job = client.reserve(timeoutSeconds);
		long getMillis = System.currentTimeMillis();

		assertNotNull(job);
		long newJobId = job.getJobId();
		assertEquals(jobId, newJobId);

		String dstString = new String(job.getData());
		assertEquals(srcString, dstString);

		long deltaSeconds = (getMillis - putMillis) / 1000;
		assertTrue(deltaSeconds >= timeoutSeconds);

		client.delete(job.getJobId());

		// now try to achieve a TIMED_OUT
		jobId = client
				.put(65536, 2 * timeoutSeconds, 120, srcString.getBytes());
		assertTrue(jobId > 0);

		job = client.reserve(timeoutSeconds);
		assertNull(job);

		popWatchedTubes(client, tubeNames);
	}

	
	public void testDelete() {

		Client client = new ClientImpl(TEST_HOST, TEST_PORT);

		Object[] tubeNames = pushWatchedTubes(client);

		String srcString = "testDelete";

		client.useTube((String) tubeNames[1]);
		long jobId = client.put(65536, 0, 120, srcString.getBytes());
		assertTrue(jobId > 0);

		Job job = client.reserve(null);
		assertNotNull(job);
		boolean ok = client.delete(job.getJobId());
		assertTrue(ok);

		// delete a second time
		ok = client.delete(job.getJobId());
		assertFalse(ok);

		popWatchedTubes(client, tubeNames);
	}

	
	public void testRelease() {

		Client client = new ClientImpl(TEST_HOST, TEST_PORT);

		Object[] tubeNames = pushWatchedTubes(client);

		String srcString = "testReserveWithTimeout";

		int timeoutSeconds = 2;

		// producer
		client.useTube((String) tubeNames[1]);
		long jobId = client.put(65536, timeoutSeconds, 120, srcString
				.getBytes());
		assertTrue(jobId > 0);

		// not found
		boolean ok = client.release(jobId, 65536, 0);
		assertFalse(ok);

		Job job = client.reserve(null);
		assertNotNull(job);
		assertEquals(jobId, job.getJobId());

		// quick release
		ok = client.release(jobId, 65536, 0);
		assertTrue(ok);

		job = client.reserve(null);
		assertNotNull(job);
		assertEquals(jobId, job.getJobId());

		ok = client.delete(jobId);
		assertTrue(ok);

		ok = client.release(jobId, 65536, 0);
		assertFalse(ok);

		popWatchedTubes(client, tubeNames);
	}

	
	public void testBuryKick() {

		Client client = new ClientImpl(TEST_HOST, TEST_PORT);

		Object[] tubeNames = pushWatchedTubes(client);

		String srcString = "testBuryKick";

		// nothing to bury
		boolean ok = false;
		ok = client.bury(0, 65536);
		assertFalse(ok);

		// producer
		client.useTube((String) tubeNames[1]);
		long jobId = client.put(65536, 0, 120, srcString.getBytes());
		assertTrue(jobId > 0);

		// we haven't reserved, so we can't bury
		ok = client.bury(jobId, 65536);
		assertFalse(ok);

		// we can bury
		Job job = client.reserve(0);
		assertNotNull(job);
		ok = client.bury(jobId, 65536);
		assertTrue(ok);

		// nothing to reserve
		job = client.reserve(0);
		assertNull(job);

		// kick nothing
		int count = client.kick(0);
		assertEquals(0, count);
		job = client.reserve(0);
		assertNull(job);

		// kick something
		count = client.kick(1);
		assertEquals(1, count);
		job = client.reserve(0);
		assertNotNull(job);
		assertEquals(jobId, job.getJobId());
		assertEquals(srcString, new String(job.getData()));

		client.delete(jobId);

		popWatchedTubes(client, tubeNames);
	}

	
	public void testTouch() {

		Client client = new ClientImpl(TEST_HOST, TEST_PORT);

		Object[] tubeNames = pushWatchedTubes(client);

		String srcString = "testTouch";
		int timeoutSeconds = 2;

		// nothing to touch
		boolean ok = false;
		ok = client.touch(0);
		assertFalse(ok);

		// producer
		client.useTube((String) tubeNames[1]);
		long jobId = client.put(65536, 0, timeoutSeconds, srcString.getBytes());
		assertTrue(jobId > 0);

		// we haven't reserved, so we can't touch
		ok = client.touch(jobId);
		assertFalse(ok);

		// reserve the job
		Job job = client.reserve(null);
		assertNotNull(job);

		// try to reserve another job
		try {
			job = client.reserve(2 * timeoutSeconds);
			fail("expected DEADLINE_SOON");
		} catch (BeanstalkException be) {
			String message = be.getMessage();
			assertEquals("DEADLINE_SOON", message);
			ok = client.touch(jobId);
			assertTrue(ok);
		} catch (Exception e) {
			fail("caught exception: " + e.getMessage());
		}

		client.delete(jobId);

		popWatchedTubes(client, tubeNames);
	}
	

	// ****************************************************************
	// Consumer methods
	// stats-related
	// ****************************************************************
	public void testListTubeUsed() {
		Client client = new ClientImpl(TEST_HOST, TEST_PORT);
		String s = client.listTubeUsed();
		assertNotNull(s);

		boolean dump = false;
		if (dump) {
			System.out.println("using tube: " + s);
		}
	}

	
	public void testListTubes() {
		Client client = new ClientImpl(TEST_HOST, TEST_PORT);
		List<String> list = client.listTubes();
		assertNotNull(list);

		boolean dump = false;
		if (dump) {
			for (String tube : list) {
				System.out.println("tube: " + tube);
			}
		}
	}

	
	public void testListTubesWatched() {
		Client client = new ClientImpl(TEST_HOST, TEST_PORT);
		List<String> list = client.listTubesWatched();
		assertNotNull(list);
		int initialWatchCount = list.size();
		assertTrue(initialWatchCount >= 1);

		String tubeName = "tube-" + UUID.randomUUID().toString();
		int watchCount = client.watch(tubeName);
		assertEquals(initialWatchCount + 1, watchCount);

		list = client.listTubesWatched();
		assertNotNull(list);
		assertEquals(watchCount, list.size());
		assertTrue(list.contains(tubeName));

		boolean dump = false;
		if (dump) {
			for (String tube : list) {
				System.out.println("watching tube: " + tube);
			}
		}

		watchCount = client.ignore(tubeName);
		assertEquals(initialWatchCount, watchCount);
		list = client.listTubesWatched();
		assertNotNull(list);
		assertEquals(initialWatchCount, list.size());
		assertFalse(list.contains(tubeName));

	}
	

	public void testStats() {
		Client client = new ClientImpl(TEST_HOST, TEST_PORT);
		Map<String, String> map = client.stats();
		assertNotNull(map);

		boolean dump = false;
		if (dump) {
			for (String key : map.keySet()) {
				System.out.println("key = " + key + ", ==> " + map.get(key));
			}
		}
	}
	

	public void testStatsTube() {

		Client client = new ClientImpl(TEST_HOST, TEST_PORT);

		Map<String, String> map = client.statsTube(null);
		assertNull(map);

		map = client.statsTube("tube-" + UUID.randomUUID().toString());
		assertNull(map);

		Object[] tubeNames = pushWatchedTubes(client);

		String srcString = "testStatsTube";

		// producer
		client.useTube((String) tubeNames[1]);
		long jobId = client.put(65536, 0, 120, srcString.getBytes());
		assertTrue(jobId > 0);

		Job job = client.reserve(null);
		assertNotNull(job);
		client.delete(jobId);

		map = client.statsTube((String) tubeNames[1]);
		assertNotNull(map);

		boolean dump = false;
		if (dump) {
			for (String key : map.keySet()) {
				System.out.println("key = " + key + ", ==> " + map.get(key));
			}
		}

		popWatchedTubes(client, tubeNames);
	}
	

	public void testStatsJob() {

		Client client = new ClientImpl(TEST_HOST, TEST_PORT);

		Map<String, String> map = client.statsJob(0);
		assertNull(map);

		Object[] tubeNames = pushWatchedTubes(client);

		String srcString = "testStatsJob";

		// producer
		client.useTube((String) tubeNames[1]);
		long jobId = client.put(65536, 0, 120, srcString.getBytes());
		assertTrue(jobId > 0);

		Job job = client.reserve(null);
		assertNotNull(job);

		map = client.statsJob(jobId);
		assertNotNull(map);

		boolean dump = false;
		if (dump) {
			for (String key : map.keySet()) {
				System.out.println("key = " + key + ", ==> " + map.get(key));
			}
		}

		client.delete(jobId);

		popWatchedTubes(client, tubeNames);
	}
	

	// ****************************************************************
	// Consumer methods
	// peek-related
	// ****************************************************************
	public void testPeek() {

		Client client = new ClientImpl(TEST_HOST, TEST_PORT);

		Object[] tubeNames = pushWatchedTubes(client);
		client.useTube((String) tubeNames[1]);

		Job job = client.peek(-1);
		assertNull(job);
		job = client.peek(0);
		assertNull(job);

		String srcString = "testPeek-";

		int nJobs = 3;
		long[] jobIds = new long[nJobs];

		// producer
		for (int i = 0; i < nJobs; ++i) {
			client.useTube((String) tubeNames[1]);
			long jobId = client.put(65536, 0, 120, (srcString + i).getBytes());
			assertTrue(jobId > 0);
			jobIds[i] = jobId;
		}

		// peek 'em once
		for (int i = 0; i < nJobs; ++i) {
			job = client.peek(jobIds[i]);
			assertNotNull(job);
			assertEquals(jobIds[i], job.getJobId());
		}

		// peek 'em again
		for (int i = 0; i < nJobs; ++i) {
			job = client.peek(jobIds[i]);
			assertNotNull(job);
			assertEquals(jobIds[i], job.getJobId());
		}

		// reserve and delete
		for (int i = 0; i < nJobs; ++i) {
			job = client.reserve(null);
			assertNotNull(job);
			assertEquals(jobIds[i], job.getJobId());
			client.delete(job.getJobId());
		}

		// peek one last time
		for (int i = 0; i < nJobs; ++i) {
			job = client.peek(jobIds[i]);
			assertNull(job);
		}

		popWatchedTubes(client, tubeNames);
	}

	
	public void testReady() {

		Client client = new ClientImpl(TEST_HOST, TEST_PORT);

		Object[] tubeNames = pushWatchedTubes(client);
		client.useTube((String) tubeNames[1]);

		String srcString = "testPeekReady-";

		int nJobs = 3;
		long[] jobIds = new long[nJobs];

		// producer
		for (int i = 0; i < nJobs; ++i) {
			client.useTube((String) tubeNames[1]);
			long jobId = client.put(65536, 0, 120, (srcString + i).getBytes());
			assertTrue(jobId > 0);
			jobIds[i] = jobId;
		}

		// peek 'em once
		Job job = null;
		for (int i = 0; i < nJobs; ++i) {
			job = client.peekReady();
			assertNotNull(job);
			assertEquals(jobIds[0], job.getJobId());
		}

		// reserve and delete
		for (int i = 0; i < nJobs; ++i) {
			job = client.reserve(null);
			assertNotNull(job);
			assertEquals(jobIds[i], job.getJobId());
			client.delete(job.getJobId());
		}

		// peek one last time
		for (int i = 0; i < nJobs; ++i) {
			job = client.peekReady();
			assertNull(job);
		}

		popWatchedTubes(client, tubeNames);
	}
	
	
	public void testDelayed() {

		Client client = new ClientImpl(TEST_HOST, TEST_PORT);

		Object[] tubeNames = pushWatchedTubes(client);
		client.useTube((String) tubeNames[1]);

		String srcString = "testPeekDelay";
		int delaySeconds = 2;

		// producer
		client.useTube((String) tubeNames[1]);
		// note we adjust delay
		long jobId = client.put(65536, delaySeconds, 120, srcString.getBytes());
		assertTrue(jobId > 0);
	
		
		// peekDelayed
		Job job = client.peekDelayed();
		assertNotNull(job);
		assertEquals(jobId, job.getJobId());
		
		try {
			Thread.sleep(delaySeconds * 1000);
		} catch (Exception e) {
			
		}
	
		// reserve and delete
		job = client.reserve(null);
		assertNotNull(job);
		assertEquals(jobId, job.getJobId());
		client.delete(job.getJobId());

		// peek one last time
		job = client.peekDelayed();
		assertNull(job);

		popWatchedTubes(client, tubeNames);
	}
	
	
	public void testBuried() {

		Client client = new ClientImpl(TEST_HOST, TEST_PORT);

		Object[] tubeNames = pushWatchedTubes(client);
		client.useTube((String) tubeNames[1]);

		String srcString = "testPeekBuried";
		
		// peekBuried
		Job job = client.peekBuried();
		assertNull(job);
		
		// producer
		long jobId = client.put(65536, 0, 120, srcString .getBytes());
		assertTrue(jobId > 0);
		
		// peekBuried
		job = client.peekBuried();
		assertNull(job);
		
		// reserve and bury
		job = client.reserve(null);
		assertNotNull(job);
		assertEquals(jobId, job.getJobId());
		client.bury(job.getJobId(), 65536);
		
		// peekBuried
		job = client.peekBuried();
		assertNotNull(job);
		assertEquals(jobId, job.getJobId());

		// delete
		client.delete(jobId);

		// peekBuried
		job = client.peekBuried();
		assertNull(job);

		popWatchedTubes(client, tubeNames);
	}
	
	public void testClose() {
		Client client = new ClientImpl(TEST_HOST, TEST_PORT);
		String s = client.listTubeUsed();
		assertNotNull(s);

		client.close();
		try {
			client.listTubeUsed();
			fail("didn't throw expected exception");
		} catch (BeanstalkException be) {
			String message = be.getMessage();
			assertEquals("Socket is closed", message);
		} catch (Exception e) {
			fail("caught exception: " + e.getMessage());
		}

		// close again
		client.close();
		try {
			client.listTubeUsed();
			fail("didn't throw expected exception");
		} catch (BeanstalkException be) {
			String message = be.getMessage();
			assertEquals("Socket is closed", message);
		} catch (Exception e) {
			fail("caught exception: " + e.getMessage());
		}
	}

	public void testUseBlockIO() {

		String remoteHost = TEST_HOST;
		int nIterations = 100;
		for (int i = 0; i < nIterations; ++i) {
			Set<Boolean> blockModes = new HashSet<Boolean>(Arrays
					.asList(new Boolean[] { false, true }));
			for (boolean useBlockIO : blockModes) {
				Client client = new ClientImpl(remoteHost, TEST_PORT,
						useBlockIO);

				Object[] tubeNames = pushWatchedTubes(client);

				String srcString = "testUseBlockIO";

				// producer
				client.useTube((String) tubeNames[1]);
				long jobId = client.put(65536, 0, 120, srcString.getBytes());
				assertTrue(jobId > 0);

				// consumer
				Job job = client.reserve(null);
				assertNotNull(job);
				long newJobId = job.getJobId();
				assertEquals(jobId, newJobId);

				String dstString = new String(job.getData());
				assertEquals(srcString, dstString);

				client.delete(job.getJobId());

				popWatchedTubes(client, tubeNames);
			}
		}
	}
	
	public void testNullArgs() {
		Client client = new ClientImpl(TEST_HOST, TEST_PORT);

		try {
			client.ignore(null);
			fail ("didn't throw");
		} catch (BeanstalkException be ) {
			assertEquals("null tubeName", be.getMessage());
		} catch (Exception e) {
			fail("caught unexpected exception: " + e.getClass().getCanonicalName() + ", " + e.getMessage() );
		}
		
		try {
			client.useTube(null);
			fail ("didn't throw");
		} catch (BeanstalkException be ) {
			assertEquals("null tubeName", be.getMessage());
		} catch (Exception e) {
			fail("caught unexpected exception: " + e.getClass().getCanonicalName() + ", " + e.getMessage() );
		}
		
		try {
			client.watch(null);
			fail ("didn't throw");
		} catch (BeanstalkException be ) {
			assertEquals("null tubeName", be.getMessage());
		} catch (Exception e) {
			fail("caught unexpected exception: " + e.getClass().getCanonicalName() + ", " + e.getMessage() );
		}
	}
	
	public void testPutPerformance() {
		Client client = new ClientImpl(TEST_HOST, TEST_PORT);
		
		Object[] tubeNames = pushWatchedTubes(client);
		client.useTube((String) tubeNames[1]);
		
		byte[] bytes = "testPutPerformance".getBytes();
		int nIterations = 10;
		long sumMillis = 0;
		
		for (int i = 0; i < nIterations; ++i) {
			long startMillis = System.currentTimeMillis();
			client.put(0, 0, 120, bytes);
            long deltaMillis = System.currentTimeMillis() - startMillis;
            sumMillis += deltaMillis;
		}
		
        long averageMillis = sumMillis / nIterations;
        assertTrue(averageMillis <= 2);
	}
	
	
	public void testIgnoreDefaultTube() {
		Client client = new ClientImpl(TEST_HOST, TEST_PORT);
		
		final String DEFAULT_TUBE = "default";
		List<String> tubeNames = client.listTubesWatched();
		assertEquals(1, tubeNames.size());
		assertEquals(DEFAULT_TUBE,tubeNames.get(0));
		
		int watchCount = client.ignore(DEFAULT_TUBE);
		assertEquals(-1, watchCount);
	}	

}
