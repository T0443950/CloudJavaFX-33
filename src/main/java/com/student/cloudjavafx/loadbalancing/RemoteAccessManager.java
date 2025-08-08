package com.student.cloudjavafx.loadbalancing;
// This is a demonstration of how to implement remote access to Docker containers.
// You will need to add the "docker-java" or a similar library to your Maven project.


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class RemoteAccessManager {

    private final DockerClient dockerClient;

    public RemoteAccessManager() {
        // Initialize the Docker client connection.
        // It's assumed that the Docker Daemon is running on the same machine.
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost("unix:///var/run/docker.sock") // For Linux
            // .withDockerHost("tcp://localhost:2375") // For Windows/Mac with Docker Desktop
            .build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .build();

        this.dockerClient = DockerClientImpl.getInstance(config, httpClient);
    }

    /**
     * Executes a command inside a Docker container and returns the output.
     * @param containerId The ID of the container.
     * @param command The command to execute (e.g., "ls -la").
     * @return The command's output as a string.
     * @throws Exception If an error occurs during execution.
     */
    public String executeCommand(String containerId, String... command) throws Exception {
        // Ensure the container is running.
        InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(containerId).exec();
        if (!containerInfo.getState().getRunning()) {
            return "Error: Container is not running.";
        }

        // Create the execution command.
        ExecCreateCmdResponse execResponse = dockerClient.execCreateCmd(containerId)
            .withAttachStdout(true)
            .withAttachStderr(true)
            .withCmd(command)
            .exec();

        // Start the command execution.
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        dockerClient.execStartCmd(execResponse.getId())
            .withDetach(false) // Wait for the command to complete.
            .exec(new com.github.dockerjava.api.async.ResultCallback.Adapter<com.github.dockerjava.api.model.Frame>() {
                @Override
                public void onNext(com.github.dockerjava.api.model.Frame frame) {
                    if (frame.getStreamType().equals(com.github.dockerjava.api.model.StreamType.STDOUT)) {
                        try {
                            stdout.write(frame.getPayload());
                        } catch (Exception e) {
                            System.err.println("Error writing to stdout: " + e.getMessage());
                        }
                    } else if (frame.getStreamType().equals(com.github.dockerjava.api.model.StreamType.STDERR)) {
                        try {
                            stderr.write(frame.getPayload());
                        } catch (Exception e) {
                            System.err.println("Error writing to stderr: " + e.getMessage());
                        }
                    }
                }
            })
            .awaitCompletion(30, TimeUnit.SECONDS); // Wait up to 30 seconds for the command to complete.

        if (stderr.size() > 0) {
            throw new Exception("Command execution failed: " + IOUtils.toString(stderr.toByteArray(), StandardCharsets.UTF_8.name()));
        }

        return IOUtils.toString(stdout.toByteArray(), StandardCharsets.UTF_8.name());
    }

    // Example of how to use the class in your application.
    public static void main(String[] args) {
        RemoteAccessManager remoteManager = new RemoteAccessManager();

        // Assume this is the ID of one of your file storage containers.
        String containerId = "my-file-storage-container-1"; // Replace with the actual container ID.

        try {
            // Example: executing "ls -la" command
            System.out.println("Executing 'ls -la' on container " + containerId + ":");
            String lsOutput = remoteManager.executeCommand(containerId, "ls", "-la");
            System.out.println(lsOutput);

            // Example: executing "touch new_file.txt"
            System.out.println("Executing 'touch new_file.txt' on container " + containerId + ":");
            remoteManager.executeCommand(containerId, "touch", "new_file.txt");
            System.out.println("File created successfully.");

            // Example: executing "cp" command (this is more complex and may require additional steps)
            // remoteManager.executeCommand(containerId, "cp", "/path/to/source", "/path/to/destination");

        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
        }
    }
}
