package com.surftools.BeanstalkClient;

/*

 Copyright 2009-2013 Robert Tykulsker 

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

import java.util.List;
import java.util.Map;

/**
 * see https://github.com/kr/beanstalkd/blob/master/doc/protocol.txt
 * 
 * 
 */
public interface Client {

	// ****************************************************************
	// Producer methods
	// ****************************************************************

	/**
	 * The "put" command is for any process that wants to insert a job into the queue.
	 * 
	 * @param priority
	 *            is an integer < 2**32. Jobs with smaller priority values will be scheduled before jobs with larger
	 *            priorities. The most urgent priority is 0; the least urgent priority is 4,294,967,295.
	 * 
	 * @param delaySeconds
	 *            is an integer number of seconds to wait before putting the job in the ready queue. The job will be in
	 *            the "delayed" state during this time.
	 * 
	 * @param timeToRun
	 *            is an integer number of seconds to allow a worker to run this job. This time is counted from the
	 *            moment a worker reserves this job. If the worker does not delete, release, or bury the job within
	 *            timeToRun seconds, the job will time out and the server will release the job. The minimum timeToRun is
	 *            1. If the client sends 0, the server will silently increase the timeToRun to 1.
	 * 
	 * @param data
	 *            is the job body -- a sequence of bytes
	 * 
	 * @return the jobId of the inserted job
	 */
	public long put(long priority, int delaySeconds, int timeToRun, byte[] data);

	/**
	 * The "use" command is for producers. Subsequent put commands will put jobs into the tube specified by this
	 * command. If no use command has been issued, jobs will be put into the tube named "default".
	 * 
	 * @param tubeName
	 */
	public void useTube(String tubeName);

	// ****************************************************************
	// Consumer methods
	// job-related
	// ****************************************************************

	/**
	 * This will return a newly-reserved job. If no job is available to be reserved, beanstalkd will wait to send a
	 * response until one becomes available. Once a job is reserved for the client, the client has limited time to run
	 * (TTR) the job before the job times out. When the job times out, the server will put the job back into the ready
	 * queue. Both the TTR and the actual time left can be found in response to the stats-job command.
	 * 
	 * 
	 * 
	 * @param timeoutSeconds
	 *            A timeout value of 0 will cause the server to immediately return either a response or TIMED_OUT. A
	 *            positive value of timeout will limit the amount of time the client will block on the reserve request
	 *            until a job becomes available.
	 * 
	 * @return the Job, or null if no job available within timeoutSeconds
	 */
	public Job reserve(Integer timeoutSeconds);

	/**
	 * 
	 The delete command removes a job from the server entirely. It is normally used by the client when the job has
	 * successfully run to completion. A client can delete jobs that it has reserved, ready jobs, delayed jobs, and jobs
	 * that are buried.
	 * 
	 * @param jobId
	 * 
	 * @return true if job deleted, false otherwise
	 */
	public boolean delete(long jobId);

	/**
	 * The release command puts a reserved job back into the ready queue (and marks its state as "ready") to be run by
	 * any client. It is normally used when the job fails because of a transitory error.
	 * 
	 * @param jobId
	 *            is the job id to release.
	 * @param priority
	 *            is a new priority to assign to the job.
	 * @param delaySeconds
	 *            is an integer number of seconds to wait before putting the job in the ready queue. The job will be in
	 *            the "delayed" state during this time.
	 * 
	 * @return true if released ok, false otherwise
	 */
	public boolean release(long jobId, long priority, int delaySeconds);

	/**
	 * The bury command puts a job into the "buried" state. Buried jobs are put into a FIFO linked list and will not be
	 * touched by the server again until a client kicks them with the "kick" command.
	 * 
	 * @param jobId
	 *            is the job id to release.
	 * @param priority
	 *            is a new priority to assign to the job.
	 * 
	 * @return true if released, false otherwise
	 */
	public boolean bury(long jobId, long priority);

	/**
	 * The "touch" command allows a worker to request more time to work on a job. This is useful for jobs that
	 * potentially take a long time, but you still want the benefits of a TTR pulling a job away from an unresponsive
	 * worker. A worker may periodically tell the server that it's still alive and processing a job (e.g. it may do this
	 * on DEADLINE_SOON). The command postpones the auto release of a reserved job until TTR seconds from when the
	 * command is issued.
	 * 
	 * @param jobId
	 *            is the ID of a job reserved by the current connection.
	 * 
	 * @return true if touched ok, false otherwise
	 */
	public boolean touch(long jobId);

	// ****************************************************************
	// Consumer methods
	// tube-related
	// ****************************************************************

	/**
	 * The "watch" command adds the named tube to the watch list for the current connection. A reserve command will take
	 * a job from any of the tubes in the watch list. For each new connection, the watch list initially consists of one
	 * tube, named "default".
	 * 
	 * @param tubeName
	 *            is a name at most 200 bytes. It specifies a tube to add to the watch list. If the tube doesn't exist,
	 *            it will be created.
	 * 
	 * @return is the integer number of tubes currently in the watch list.
	 */
	public int watch(String tubeName);

	/**
	 * The "ignore" command is for consumers. It removes the named tube from the watch list for the current connection.
	 * 
	 * @param tubeName
	 *            the name of the tube to be ignored.
	 * 
	 * @return the integer number of tubes currently in the watch list, or -1, if the client attempts to ignore the only
	 *         tube in its watch list.
	 */
	public int ignore(String tubeName);

	// ****************************************************************
	// Consumer methods
	// peek-related
	// ****************************************************************

	/**
	 * The peek commands let the client inspect a job in the system.
	 * 
	 * @param jobId
	 * 
	 * @return return job or null if not found
	 */
	public Job peek(long jobId);

	/**
	 * 
	 * @return return the next ready job.
	 */
	public Job peekReady();

	/**
	 * 
	 * @return return the delayed job with the shortest delay left.
	 */
	public Job peekDelayed();

	/**
	 * 
	 * @return return the next job in the list of buried jobs.
	 */
	public Job peekBuried();

	/**
	 * The kick command applies only to the currently used tube. It moves jobs into the ready queue. If there are any
	 * buried jobs, it will only kick buried jobs. Otherwise it will kick delayed jobs
	 * 
	 * @param count
	 *            is an integer upper bound on the number of jobs to kick. The server will kick no more than <bound>
	 *            jobs.
	 * 
	 * @return the integer indicating the number of jobs actually kicked.
	 */
	public int kick(int count);

	/**
	 * The kick-job command is a variant of kick that operates with a single job identified by its job id. If the given
	 * job id exists and is in a buried or delayed state, it will be moved to the ready queue of the the same tube where
	 * it currently belongs.
	 * 
	 * @param jobId
	 *            is the job id to kick.
	 * 
	 * @return true if kicked ok, otherwise false.
	 */
	public boolean kickJob(long jobId);

	// ****************************************************************
	// Consumer methods
	// stats-related
	// ****************************************************************

	/**
	 * The stats-job command gives statistical information about the specified job if it exists. Its form is:
	 * 
	 * @param jobId
	 *            is a job id.
	 * 
	 * @return a Map with the following keys:
	 * 
	 *         - "id" is the job id
	 * 
	 *         - "tube" is the name of the tube that contains this job
	 * 
	 *         - "state" is "ready" or "delayed" or "reserved" or "buried"
	 * 
	 *         - "pri" is the priority value set by the put, release, or bury commands.
	 * 
	 *         - "age" is the time in seconds since the put command that created this job.
	 * 
	 *         - "time-left" is the number of seconds left until the server puts this job into the ready queue. This
	 *         number is only meaningful if the job is reserved or delayed. If the job is reserved and this amount of
	 *         time elapses before its state changes, it is considered to have timed out.
	 * 
	 *         - "file" is the number of the earliest binlog file containing this job. If -b wasn't used, this will be
	 *         0.
	 * 
	 *         - "reserves" is the number of times this job has been reserved.
	 * 
	 *         - "timeouts" is the number of times this job has timed out during a reservation.
	 * 
	 *         - "releases" is the number of times a client has released this job from a reservation.
	 * 
	 *         - "buries" is the number of times this job has been buried.
	 * 
	 *         - "kicks" is the number of times this job has been kicked.
	 */
	public Map<String, String> statsJob(long jobId);

	/**
	 * he stats-tube command gives statistical information about the specified tube if it exists.
	 * 
	 * @param tubeName
	 *            is a name at most 200 bytes. Stats will be returned for this tube.
	 * 
	 * @return a Map with the following keys
	 * 
	 *         - "name" is the tube's name.
	 * 
	 *         - "current-jobs-urgent" is the number of ready jobs with priority < 1024 in this tube.
	 * 
	 *         - "current-jobs-ready" is the number of jobs in the ready queue in this tube.
	 * 
	 *         - "current-jobs-reserved" is the number of jobs reserved by all clients in this tube.
	 * 
	 *         - "current-jobs-delayed" is the number of delayed jobs in this tube.
	 * 
	 *         - "current-jobs-buried" is the number of buried jobs in this tube.
	 * 
	 *         - "total-jobs" is the cumulative count of jobs created in this tube in the current beanstalkd process.
	 * 
	 *         - "current-using" is the number of open connections that are currently using this tube.
	 * 
	 *         - "current-waiting" is the number of open connections that have issued a reserve command while watching
	 *         this tube but not yet received a response.
	 * 
	 *         - "current-watching" is the number of open connections that are currently watching this tube.
	 * 
	 *         - "pause" is the number of seconds the tube has been paused for.
	 * 
	 *         - "cmd-delete" is the cumulative number of delete commands for this tube
	 * 
	 *         - "cmd-pause-tube" is the cumulative number of pause-tube commands for this tube.
	 * 
	 *         - "pause-time-left" is the number of seconds until the tube is un-paused.
	 */
	public Map<String, String> statsTube(String tubeName);

	/**
	 * The stats command gives statistical information about the system as a whole.
	 * 
	 * @return a Map with the following keys
	 * 
	 *         - "current-jobs-urgent" is the number of ready jobs with priority < 1024.
	 * 
	 *         - "current-jobs-ready" is the number of jobs in the ready queue.
	 * 
	 *         - "current-jobs-reserved" is the number of jobs reserved by all clients.
	 * 
	 *         - "current-jobs-delayed" is the number of delayed jobs.
	 * 
	 *         - "current-jobs-buried" is the number of buried jobs.
	 * 
	 *         - "cmd-put" is the cumulative number of put commands.
	 * 
	 *         - "cmd-peek" is the cumulative number of peek commands.
	 * 
	 *         - "cmd-peek-ready" is the cumulative number of peek-ready commands.
	 * 
	 *         - "cmd-peek-delayed" is the cumulative number of peek-delayed commands.
	 * 
	 *         - "cmd-peek-buried" is the cumulative number of peek-buried commands.
	 * 
	 *         - "cmd-reserve" is the cumulative number of reserve commands.
	 * 
	 *         - "cmd-use" is the cumulative number of use commands.
	 * 
	 *         - "cmd-watch" is the cumulative number of watch commands.
	 * 
	 *         - "cmd-ignore" is the cumulative number of ignore commands.
	 * 
	 *         - "cmd-delete" is the cumulative number of delete commands.
	 * 
	 *         - "cmd-release" is the cumulative number of release commands.
	 * 
	 *         - "cmd-bury" is the cumulative number of bury commands.
	 * 
	 *         - "cmd-kick" is the cumulative number of kick commands.
	 * 
	 *         - "cmd-stats" is the cumulative number of stats commands.
	 * 
	 *         - "cmd-stats-job" is the cumulative number of stats-job commands.
	 * 
	 *         - "cmd-stats-tube" is the cumulative number of stats-tube commands.
	 * 
	 *         - "cmd-list-tubes" is the cumulative number of list-tubes commands.
	 * 
	 *         - "cmd-list-tube-used" is the cumulative number of list-tube-used commands.
	 * 
	 *         - "cmd-list-tubes-watched" is the cumulative number of list-tubes-watched commands.
	 * 
	 *         - "cmd-pause-tube" is the cumulative number of pause-tube commands.
	 * 
	 *         - "job-timeouts" is the cumulative count of times a job has timed out.
	 * 
	 *         - "total-jobs" is the cumulative count of jobs created.
	 * 
	 *         - "max-job-size" is the maximum number of bytes in a job.
	 * 
	 *         - "current-tubes" is the number of currently-existing tubes.
	 * 
	 *         - "current-connections" is the number of currently open connections.
	 * 
	 *         - "current-producers" is the number of open connections that have each issued at least one put command.
	 * 
	 *         - "current-workers" is the number of open connections that have each issued at least one reserve command.
	 * 
	 *         - "current-waiting" is the number of open connections that have issued a reserve command but not yet
	 *         received a response.
	 * 
	 *         - "total-connections" is the cumulative count of connections.
	 * 
	 *         - "pid" is the process id of the server.
	 * 
	 *         - "version" is the version string of the server.
	 * 
	 *         - "rusage-utime" is the cumulative user CPU time of this process in seconds and microseconds.
	 * 
	 *         - "rusage-stime" is the cumulative system CPU time of this process in seconds and microseconds.
	 * 
	 *         - "uptime" is the number of seconds since this server process started running.
	 * 
	 *         - "binlog-oldest-index" is the index of the oldest binlog file needed to store the current jobs.
	 * 
	 *         - "binlog-current-index" is the index of the current binlog file being written to. If binlog is not
	 *         active this value will be 0.
	 * 
	 *         - "binlog-max-size" is the maximum size in bytes a binlog file is allowed to get before a new binlog file
	 *         is opened.
	 * 
	 *         - "binlog-records-written" is the cumulative number of records written to the binlog.
	 * 
	 *         - "binlog-records-migrated" is the cumulative number of records written as part of compaction.
	 * 
	 *         - "id" is a random id string for this server process, generated when each beanstalkd process starts.
	 * 
	 *         - "hostname" the hostname of the machine as determined by uname.
	 */
	public Map<String, String> stats();

	/**
	 * The list-tubes command returns a list of all existing tubes.
	 * 
	 * @return a list of all existing tubes, never null
	 */
	public List<String> listTubes();

	/**
	 * The list-tube-used command returns the tube currently being used by the client.
	 * 
	 * @return the name of the tube currently being used
	 */
	public String listTubeUsed();

	/**
	 * The list-tubes-watched command returns a list tubes currently being watched by the client.
	 * 
	 * @return a list of all tubes watched, never null
	 */
	public List<String> listTubesWatched();

	/**
	 * The pause-tube command can delay any new job being reserved for a given time.
	 * 
	 * @param tubeName
	 *            is the tube to pause
	 * @param pause
	 *            is an integer number of seconds to wait before reserving any more jobs from the queue
	 * 
	 * @return true if paused ok, false otherwise
	 */
	public boolean pauseTube(String tubeName, int pauseSeconds);

	// ****************************************************************
	// Client methods
	// ****************************************************************
	/**
	 * @return the version of this JavaBeanstalkd Client
	 */
	public String getClientVersion();

	/**
	 * @return the version of the beanstalkd daemon
	 */
	public String getServerVersion();

	/**
	 * close underlying connection to beanstalkd
	 */
	public void close();

	/**
	 * is the client using one unique connection per thread or a single shared connection?
	 * 
	 * @return true if one unique connection per thread or false for a single shared connection
	 */
	public boolean isUniqueConnectionPerThread();

	/**
	 * 
	 * is the client using one unique connection per thread or a single shared connection?
	 * 
	 * @param uniqueConnectionPerThread
	 *            true for a unique connection per thread, false for a single shared connection
	 */
	public void setUniqueConnectionPerThread(boolean uniqueConnectionPerThread);

}
