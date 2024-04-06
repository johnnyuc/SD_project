# SD_project

## Description
This project is a Search Engine that uses a distributed architecture to provide a scalable and fault-tolerant solution.
Based on Java, it focuses on synchronizing data between different system components:
* Gateway
* Client
* URLQueue
* Downloaders
* Index Storage Barrels

The project is part of the Distributed Systems course at the University of Coimbra, and it is developed by:
* Johnny Fernandes 2021190668
* Miguel Leopoldo 2021225940

## Installation and usage
1. Clone the repository: `git clone https://github.com/johnnyuc/SD_project.git`
2. Navigate to the project directory: `cd SD_project`
3. Open it in your favorite IDE (IntelliJ IDEA, Eclipse, etc.)
4. Run the URLQueue class first to start the URLQueue server
5. Run the Gateway class to start the Gateway server using the URLQueue server: `-qadd <URLQueue_IP>`
6. Run the Client class to start the Client server using the Gateway server: `-ip <Gateway_IP>`
7. Run the IndexStorageBarrels class to start the IndexStorageBarrels server using the Gateway server: `-s <[OPTIONAL]TF-IDF_SORT> -id <ID> -db <DATABASE_NAME> -dmcast <[DOWNLOADERS]MULTICAST_GROUP> -dport <[DOWNLOADERS]MULTICAST_PORT> -smcast <[SYNC]MULTICAST_GROUP> -sport <[SYNC]MULTICAST_PORT> -mcastadd <NETWORK_INTERFACE_IP> -badd <ISB_IP> <GATEWAY_IP>`
8. Run the Downloaders class to start the Downloaders server using the Gateway server: `-d <THREAD_NUMBER> -ip <URL_QUEUE_IP> -mcast <MULTICAST_GROUP> -mcastport <MULTICAST_PORT> -mcastinterface <NETWORK_INTERFACE_IP>`

## Example usage for localhost
1. Run the URLQueue class: `java URLQueue`
2. Run the Gateway class: `java Gateway -qadd localhost`
3. Run the Client class: `java Client -ip localhost`
4. Run the IndexStorageBarrels class: `java IndexStorageBarrel -s -id 1 -db testBarrel -dmcast 224.3.2.1 -dport 4321 -smcast 224.4.3.2 -sport 12346 -mcastadd localhost -badd localhost -gadd localhost`
5. Run the Downloaders class: `java Downloader -d 5 -ip localhost -mcast 224.3.2.1 -mcastport 4321 -mcastinterface localhost`