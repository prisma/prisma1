#!/usr/bin/env bash
TARGET_DIRECTORY=target/prisma2-preview-package
PREVIEW_FOLDER=prisma2-preview-package
SQLITE_DB_PATH=../db/Chinook.sqlite

rm -rf $TARGET_DIRECTORY
cp -R $PREVIEW_FOLDER target/
cp playground.html $TARGET_DIRECTORY/
mkdir $TARGET_DIRECTORY/db

cargo build --release
cp target/release/prisma $TARGET_DIRECTORY/

if [ ! -f $SQLITE_DB_PATH ]; then
    echo "Sqlite database not downloaded yet. Downloading it now."
    curl -L https://github.com/lerocha/chinook-database/raw/master/ChinookDatabase/DataSources/Chinook_Sqlite.sqlite -o $SQLITE_DB_PATH
fi
cp $SQLITE_DB_PATH $TARGET_DIRECTORY/db/Chinook.db

if [ ! -f $SCHEMA_INFERRER_PATH ]; then
    echo "Schema Inferrer Bin not build yet. Building it now."
    cd .. 
    sbt schema-inferrer-bin/prisma-native-image:packageBin
    cd -
fi
cp $SCHEMA_INFERRER_PATH $TARGET_DIRECTORY/

echo "Preview package ready in directory $TARGET_DIRECTORY"
cd target/$PREVIEW_FOLDER && zip -r $PREVIEW_FOLDER.zip * && cd -
echo "Zip available in $TARGET_DIRECTORY/$PREVIEW_FOLDER.zip"