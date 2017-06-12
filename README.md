# KittenWhisker

`KittenWhisker` is a tool to facilitate the performance debugging of Spark applications.
 It generates symbol files for JIT compiled methods of Drivers and Executors which can be further
  used by Linux [`perf`](https://perf.wiki.kernel.org/index.php/Main_Page) tools.

In this file, I will show how to [configure KittenWhisker](#configure) and use KittenWhisker.
 
 
 
 

##  <a name="configure">Configure KittenWhisker</a> 

### Prepare Slaves File

Before you install KittenWhisker, you have to prepare a `slaves` files containing IP address of all nodes
 in your cluster (or all nodes which can be used to run Spark Drivers/Executors)
 
### Clone KittenWhisker repository and install it

1. First you have to clone the repo from github

`git clone https://github.com/CodingCat/KittenWhisker.git`

2. Then you can compile it with maven (ensure that you installed `cmake` beforehand) 

`cd KittenWhisker; mvn package`

### Install Depended Tools 

Then you need to install several tools depended by `KittenWhisker` 

1. Install Linux's Perf tool in every node of the cluster

    Depends on your OS, you have various ways to do it, e.g. in Ubuntu, you can do it by 
    
    ```
    sudo apt-get install linux-tools-common linux-tools-generic linux-tools-`uname -r`
    ```
    
    You can also install via a helper script provided by KittenWhisker, run the following command in
     the root path of KittenWhisker
    
    ```
    ./install_packages.sh username_to_login_to_machines 
    ```
    
    This script will read slaves file, login to each machine specified in the file with the
     username you provided and install perf tool. (Assuming the machine is with Ubuntu OS)  
    
2. Install JDK debugging symbols (if you are using OpenJDK)

   If you are using OpenJDK, JDK debugging symbols are used to interpret the JVM runtime function names.
    To install it, you can run 
    
    ```
    sudo apt-get install openjdk-8-dbg
    ```
    
    Or with the helper script in KittenWhisker, you can simply do
    
    ```
    ./install_packages.sh username_to_login_to_machines 
    ```
    
    with the assumption that you are using Ubuntu. 
    
3. Install Flamegraph tool (just need to be done in the machine you currently login in )

   Install Flamegraph tool is as simple as clone Brendan's repo and setup environment variable for
    KittenWhisker. Do the following
    
    ```
    git clone https://github.com/brendangregg/FlameGraph.git
    
    cd FlameGraph; echo "export FLAMEGRAPH_DIR=`pwd`" >> ~/.bashrc
    ```

## Acknowledgement

This work cannot be done without the existing tools from other developers. It uses [perf-map-agent by
 Johannes Rudolph](https://github.com/jvm-profiling-tools/perf-map-agent), and it also grabs some code
 from [Min Zhou's perfj](https://github.com/coderplay/perfj). The flame graph generation part is, of course,
 based on [Brendan Gregg's FlameGraph Tool](https://github.com/brendangregg/FlameGraph)


## Disclaimer

It is not a software released by Microsoft and the code is very experimental!
 You will use it at your own risk. You have been warned!