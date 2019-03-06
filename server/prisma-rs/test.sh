# tests all rust projects. 
# We are not using cargo workspaces because this is causing troubles with JNA. 
# With workspaces the dylibs for query-engine are not bitwise identical to the ones without workspaces.
function test() {
    folder_to_test=$1
    echo "will run cargo test in $folder_to_test"
    current_dir=$PWD
    cd $folder_to_test
    cargo test
    cd $current_dir
}

for lib in libs/*/ ; do
    test $lib
done
test "migration-engine"
test "query-engine"
