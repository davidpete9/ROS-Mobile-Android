package com.schneewittchen.rosandroid.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.schneewittchen.rosandroid.model.entities.SSHEntity;
import com.schneewittchen.rosandroid.model.repositories.SshRepositoryImpl;


/**
 * TODO: Description
 *
 * @author Nils Rottmann
 * @version 1.0.0
 * @created on 18.03.20
 * @updated on 04.06.20
 * @modified by Nils Rottmann
 */

public class SshViewModel extends AndroidViewModel {

    SshRepositoryImpl sshRepositoryImpl;
    private final LiveData<SSHEntity> currentSSH;


    public SshViewModel(@NonNull Application application) {
        super(application);
        sshRepositoryImpl = SshRepositoryImpl.getInstance(application);
        currentSSH = sshRepositoryImpl.getCurrentSSH();
    }

    public void setSshIp(String ipString) {
        Log.i("TST","CurrentSSH: "+currentSSH.getValue());
        SSHEntity ssh = currentSSH.getValue();
        ssh.ip = ipString;
        sshRepositoryImpl.updateSSHConfig(ssh);
    }

    public void setSshPort(String portString) {
        int port = 22;

        try {
            port = Integer.parseInt(portString);
        } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
        }

        SSHEntity ssh = currentSSH.getValue();
        ssh.port = port;
        sshRepositoryImpl.updateSSHConfig(ssh);
    }

    public void setSshUsername(String usernameString) {
        SSHEntity ssh = currentSSH.getValue();
        ssh.username = usernameString;
        sshRepositoryImpl.updateSSHConfig(ssh);
    }

    public void setSshHostPort(String sshHostPort) {
        SSHEntity ssh = currentSSH.getValue();
        int host_port = -1;
        try {
            host_port = Integer.parseInt(sshHostPort);
        } catch (NumberFormatException nfe) {
        nfe.printStackTrace();
        }
        ssh.host_port = host_port;
        sshRepositoryImpl.updateSSHConfig(ssh);
    }

    public void setSshRemotePort(String sshRemotePort) {
        SSHEntity ssh = currentSSH.getValue();
        int remote_port = -1;
        try {
            remote_port = Integer.parseInt(sshRemotePort);
        } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
        }
        ssh.remote_port = remote_port;

        // when remote port is not invalid we shoud set remote host to the remote's ip
        // default.. TODO
        if (remote_port > 0) {
            ssh.remote_host = ssh.ip;
        }
        sshRepositoryImpl.updateSSHConfig(ssh);
    }

    public void setSshPassword(String passwordString) {
        SSHEntity ssh = currentSSH.getValue();
        ssh.password = passwordString;
        sshRepositoryImpl.updateSSHConfig(ssh);
    }


    public void connectViaSSH() {
        sshRepositoryImpl.startSession();
    }

    public void stopSsh() {
        sshRepositoryImpl.stopSession();
    }

    public void sendViaSSH(String message) {
        sshRepositoryImpl.sendMessage(message);
    }

    public void abortAction() {sshRepositoryImpl.abort();}

    public LiveData<Boolean> isConnected() {
        return sshRepositoryImpl.isConnected();
    }

    public LiveData<String> getOutputData() {
        return sshRepositoryImpl.getOutputData();
    }

    public LiveData<SSHEntity> getSSH() {
        return sshRepositoryImpl.getCurrentSSH();
    }

}
