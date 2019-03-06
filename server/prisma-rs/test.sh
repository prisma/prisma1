# tests all rust projects. 
# We are not using cargo workspaces because this is causing troubles with JNA. 
# With workspaces the dylibs for query-engine are not bitwise identical to the ones without workspaces.
for lib in libs/*/ ; do
    cd $lib && cargo test && cd -
done
cd migration-engine && cargo test && cd -
cd query-engine && cargo test && cd -