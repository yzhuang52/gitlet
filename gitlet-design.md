# Gitlet Design Document

**Name**: Yan Zhuang

## Classes and Data Structures

### Class 1
#### Main
This is the entry point to our program. 
It takes in arguments from the command line and based on the command 
(the first element of the args array) calls the corresponding command in 
gitlet which will actually execute the logic of the command. 
It also validates the arguments based on the command to ensure that 
enough arguments were passed in.
#### Fields
This class has no fields and hence no associated state: 
it simply validates arguments and defers the execution to the Repository class.
### Class 2
#### Repository
This is where the main logic of our program will live. 
This file will handle all of the actual gitlet commands by reading/writing from/to the correct file,
setting up persistence, and additional error checking.

It will also be responsible for setting up all persistence within capers.
This includes creating the .gitlet folder as well as the folder and file
where we store all Dog objects and the current story.
#### Fields

1. Field 1
2. Field 2

### Class 3
#### Commit


#### Fields
1. message
2. currentDate
3. timestamp
4. ID
5. parentID
6. blobPathList

### Class 4
#### Blob

#### Fields
1. fileID
2. fileContent


## Algorithms

## Persistence
<pre>
└── .gitlet
    └── objects
        ├── commits
        ├── blobs
    ├── refs
        ├── heads
            ├── master
    ├── HEAD
    ├── addStaging
    ├── rmStaging
    ├── index
</pre>

