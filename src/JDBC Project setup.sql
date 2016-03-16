/*
    Author: Steven McCracken

    SQL script defines tables and relationships
    between tables for JDBC Project.
 */

-- Define RecordingGroups table
CREATE TABLE RecordingGroups (
    GroupName   VARCHAR(45) NOT NULL,
    LeadSinger  VARCHAR(60) NOT NULL,
    YearFormed  INTEGER     NOT NULL,
    Genre       VARCHAR(60) NOT NULL );

-- Define Albums table
CREATE TABLE Albums (
    AlbumTitle      VARCHAR(60) NOT NULL,
    GroupName       VARCHAR(45) NOT NULL,
    NumberOfSongs   INTEGER     NOT NULL,
    StudioName      VARCHAR(50) NOT NULL,
    DateRecorded    DATE        NOT NULL,
    Length          INTEGER     NOT NULL);

-- Define RecordingStudios table
CREATE TABLE RecordingStudios (
    StudioName      VARCHAR(50)     NOT NULL,
    StudioAddress   VARCHAR(100)    NOT NULL,
    StudioOwner     VARCHAR(60)     NOT NULL,
    StudioPhone     VARCHAR(13)     NOT NULL);

-- The primary key of RecordingGroups is their group name
ALTER TABLE RecordingGroups ADD CONSTRAINT
    RecordingGroups_PK PRIMARY KEY (GroupName);

-- The primary key of RecordingStudios is their studio name
ALTER TABLE RecordingStudios ADD CONSTRAINT
    RecordingStudios_PK PRIMARY KEY (StudioName);

/*
    Albums has a foreign key of group name from RecordingGroups.
    Each RecordingGroup has 1 to many Albums.
    Each Album has 1 and only 1 RecordingGroup.
*/
ALTER TABLE Albums ADD CONSTRAINT
    RecordingGroups_Albums_FK FOREIGN KEY (GroupName)
    REFERENCES RecordingGroups;

/*
    Albums has a foreign key of studio name from RecordingGroups.
    Each RecordingStudio has 1 to many Albums.
    Each Album has 1 and only 1 RecordingStudio.
*/
ALTER TABLE Albums ADD CONSTRAINT
    RecordingStudios_Albums_FK FOREIGN KEY (StudioName)
    REFERENCES RecordingStudios;

/*
    The primary key of Albums is it's album title and group name.
    This is a many-to-many relationship between RecordingGroups and
    RecordingStudios with history.
*/
ALTER TABLE Albums ADD CONSTRAINT
    Albums_PK PRIMARY KEY (AlbumTitle, GroupName);