Install on Centos 8
=====================================================

.. contents:: Contents


Overview
--------
This document describes the packages that must be installed in order to build phoebus-olog, 
on a new Centos 8 Stream system.

For other versions of Linux the package manager and package names may be different, 
but the requirements are likely to be the same.
This procedure was verified in November 2021 on a RaspberryPi 4.

Add the Extra Packages for Enterprise Linux (EPEL) site for the dnf package manager.
This site has additional packages that are needed

.. code-block:: bash

  sudo dnf install epel-release

Enable the powertools repository by running

.. code-block:: bash
   
  sudo dnf config-manager --set-enabled powertools

Some packages don't ship with CentOS, or require newer versions.
Instructions are included on how to create new repos for these, and all
of the repos are set to disabled by default.  Enable them if you want
automatic updates.

Packages required to build phoebus-olog
---------------------------------------

.. code-block:: bash

  sudo dnf install git java-11-openjdk-headless java-11-openjdk-devel maven nodejs checkpolicy


elasticsearch
----------------------------------

.. code-block:: bash

  sudo bash -c 'cat << EOF > /etc/yum.repos.d/elasticsearch.repo
  [elasticsearch]
  name=Elasticsearch repository for 6.x packages
  baseurl=https://artifacts.elastic.co/packages/6.x/yum
  gpgcheck=1
  gpgkey=https://artifacts.elastic.co/GPG-KEY-elasticsearch
  enabled=0
  autorefresh=1
  type=rpm-md
  EOF'

  sudo rpm --import https://artifacts.elastic.co/GPG-KEY-elasticsearch
  sudo dnf install --enablerepo=elasticsearch elasticsearch-6.8.4

Mongodb
---------------------------------------

For some reason, mongodb releases their mongodb-database-tools in the
arm64 directory for aarch64.  The second baseurl line below will need to
be changed if you're running on x86_64.

Additionally, mongodb RPMs 5.0 DO NOT work on the rPi 4, so the
instructions below are for 4.4.  Compiling from source works fine.

.. code-block:: bash

  sudo bash -c 'cat << EOF > /etc/yum.repos.d/mongodb-org-5.0.repo
  [mongodb-org-4.4]
  name=MongoDB Repository
  baseurl=https://repo.mongodb.org/yum/redhat/\$releasever/mongodb-org/4.4/\$arch/
  gpgcheck=1
  enabled=0
  gpgkey=https://www.mongodb.org/static/pgp/server-4.4.asc

  [mongodb-org-devel]
  name=MongoDB Development Tools Repository
  baseurl=https://repo.mongodb.org/yum/redhat/\$releasever/mongodb-org/development/arm64/
  gpgcheck=0
  enabled=0
  EOF'

  sudo rpm --import https://www.mongodb.org/static/pgp/server-4.4.asc
  sudo dnf --enablerepo=mongodb-org-4.4 --enablerepo=mongodb-org-devel install mongodb-org mongodb-mongosh


Configuration and startup
-------------------------------

Elasticsearch and others don't like running with Java 1.8.0, so we have to change
it by using alternatives.

.. code-block:: bash

  sudo alternatives --set java $(alternatives --display java | grep 'family  java-11-openjdk' | cut -d' ' -f1)
  sudo alternatives --set javac $(alternatives --display javac |grep 'family java-11-openjdk' |cut -d' ' -f1)
  
And since elasticsearch assumes that you're running on x64, we need to
disable AVX2 extensions.  Edit the last line of
```/etc/elasticsearch/jvm.options`` to remove comment out

.. code-block:: bash

  # temporary workaround for C2 bug with JDK 10 on hardware with AVX-512
  # 10-:-XX:UseAVX=2

Also, elasticsearch X-Pack and Machine Learning aren't available on rPi,
so we can disable them by editing /etc/elasticsearch/elasticsearch.yml.
Add the following line:

.. code-block:: bash

  xpack.ml.enabled: false


Run the following to start elasticsearch:

.. code-block:: bash

  sudo systemctl daemon-reload
  sudo systemctl enable elasticsearch.service
  sudo systemctl start elasticsearch.service

Wait at least 30 seconds for elasticsearch to start.  You can test
functionality by running:

.. code-block:: bash

  curl -X GET http://localhost:9200/

Which should return something like this:

.. code-block:: bash

  {
    "name" : "lDXybyO",
    "cluster_name" : "elasticsearch",
    "cluster_uuid" : "puoQXk10RrSCPGhMPv_N_Q",
    "version" : {
      "number" : "6.8.4",
      "build_flavor" : "default",
      "build_type" : "rpm",
      "build_hash" : "bca0c8d",
      "build_date" : "2019-10-16T06:19:49.319352Z",
      "build_snapshot" : false,
      "lucene_version" : "7.7.2",
      "minimum_wire_compatibility_version" : "5.6.0",
      "minimum_index_compatibility_version" : "5.0.0"
    },
    "tagline" : "You Know, for Search"
  }


MongoDB requires proper SELinux configuration.  Create a custom policy
file for memory access:

.. code-block:: bash

  cat > mongodb_cgroup_memory.te <<EOF
  module mongodb_cgroup_memory 1.0;

  require {
      type cgroup_t;
      type mongod_t;
      class dir search;
      class file { getattr open read };
  }

  #============= mongod_t ==============
  allow mongod_t cgroup_t:dir search;
  allow mongod_t cgroup_t:file { getattr open read };
  EOF

And one for network access:

.. code-block:: bash

  cat > mongodb_proc_net.te <<EOF
  module mongodb_proc_net 1.0;

  require {
      type proc_net_t;
      type mongod_t;
      class file { open read };
  }
  
  #============= mongod_t ==============
  allow mongod_t proc_net_t:file { open read };
  EOF

Next, install the policies by running:

.. code-block:: bash

  checkmodule -M -m -o mongodb_cgroup_memory.mod mongodb_cgroup_memory.te
  checkmodule -M -m -o mongodb_proc_net.mod mongodb_proc_net.te
  semodule_package -o mongodb_cgroup_memory.pp -m mongodb_cgroup_memory.mod
  semodule_package -o mongodb_proc_net.pp -m mongodb_proc_net.mod
  sudo semodule -i mongodb_cgroup_memory.pp
  sudo semodule -i mongodb_proc_net.pp

And the following to start mongodb:

.. code-block:: bash

  sudo systemctl daemon-reload
  sudo systemctl enable mongod
  sudo systemctl start mongod

Now make sure mongodb is actually doing something by running ``mongosh``.
The output when I launch it looks like this:

.. code-block:: bash

  Current Mongosh Log ID: 618150561d02e31b65de48fb
  Connecting to:          mongodb://127.0.0.1:27017/?directConnection=true&serverSelectionTimeoutMS=2000
  Using MongoDB:          4.4.10
  Using Mongosh:          1.1.1

  For mongosh info see: https://docs.mongodb.com/mongodb-shell/

  ------
    The server generated these startup warnings when booting:
    2018-06-22T11:12:25.057+00:00: Using the XFS filesystem is strongly recommended with the WiredTiger storage engine. See http://dochub.mongodb.org/core/prodnotes-filesystem
    2021-11-01T20:44:31.639+00:00: Access control is not enabled for the database. Read and write access to data and configuration is unrestricted
  ------

  test>

Just type ```quit`` to exit.
Finally, set up JAVA_HOME.

.. code-block:: bash

  export JAVA_HOME="/usr/lib/jvm/java-11"

End!