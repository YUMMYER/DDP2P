#!/bin/bash
# like: 1.0.0
VERSION=$1
# like: http://debatedecide.org/DD
BASEURL=$2
DATE=00000000000000.000Z
EMPTY_SIGN=0000
EMPTY_HASH=0000
EMPTY_PKHASH=0000
SCRIPT=update
WORK_DIR=0.0.00
VERSION_INFO_RAW=DD_Updates
VERSION_INFO_SIGNED=${VERSION_INFO_RAW}.signed
TRUSTED_KEY=Trusted64.sk
DOWNLOADABLES=3
TESTER_NAME_FILE=$3
RELEASE_XML_FILE=$4
TESTS_LIST=$5
QOT=$6
ROT=$7

echo "TESTS_LIST" ${TESTS_LIST}
echo "QOT" ${QOT}
echo "ROT" ${ROT}

SOURCE="${BASH_SOURCE[0]}"
DIR_ALL="$( dirname "$SOURCE" )"
while [ -h "$SOURCE" ]
do 
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
  DIR="$( cd -P "$( dirname "$SOURCE"  )" && pwd )"
done
DIR_ALL="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
cd ${DIR_ALL}

cd ${WORK_DIR}
rm -f ddp2p_${VERSION}.jar
# jar cf test.jar `find . -not -path "*/.svn/*" -not -type d`
cp -r scripts _scripts
cp -r jars _jars
cp -r plugins _plugins
find scripts -name ".svn" -exec rm -rf {} \;
find jars -name ".svn" -exec rm -rf {} \;
find plugins -name ".svn" -exec rm -rf {} \;

jar cvf ddp2p_${VERSION}.jar *sqlite dd_* unit_test.* dir_run.* scripts plugins jars
jar uvfm ddp2p_${VERSION}.jar jarmanifest UnitZipper.class

rm -rf scripts jars plugins
mv _scripts scripts
mv _jars jars
mv _plugins plugins


cp ${SCRIPT}.sh tmp
#echo "VERSION=${VERSION}" >${SCRIPT}.sh
#cat tmp | egrep -v "^VERSION=" >>${SCRIPT}.sh
cat tmp | sed "s/^VERSION=.*/VERSION=${VERSION}/g" >${SCRIPT}.sh

rm tmp
cp ${SCRIPT}.bat tmp
#echo "SET VERSION=${VERSION}" >${SCRIPT}.bat
#cat tmp | egrep -v "^SET VERSION" >>${SCRIPT}.bat
cat tmp | sed "s/^SET VERSION=.*/SET VERSION=${VERSION}#/g" | tr "#" "\r" >${SCRIPT}.bat
rm tmp


cd ..
mkdir -p ${VERSION}

cp "${WORK_DIR}/ddp2p_${VERSION}.jar" "${VERSION}/"
cp ${WORK_DIR}/update* "${VERSION}/"

cd ${VERSION}
echo "START" >${VERSION_INFO_RAW}
echo "${VERSION}" >>${VERSION_INFO_RAW}
echo "${DATE}" >>${VERSION_INFO_RAW}
#echo "${EMPTY_SIGN}" >>${VERSION_INFO_RAW}
echo "${DOWNLOADABLES}" >>${VERSION_INFO_RAW}
echo "${SCRIPT}" >>${VERSION_INFO_RAW}

echo "ddp2p_${VERSION}.jar" >>${VERSION_INFO_RAW}
echo "${BASEURL}/${VERSION}/ddp2p_${VERSION}.jar" >>${VERSION_INFO_RAW}
echo "${EMPTY_HASH}" >>${VERSION_INFO_RAW}

echo "${SCRIPT}.sh" >>${VERSION_INFO_RAW}
echo "${BASEURL}/${VERSION}/${SCRIPT}.sh" >>${VERSION_INFO_RAW}
echo "${EMPTY_HASH}" >>${VERSION_INFO_RAW}

echo "${SCRIPT}.bat" >>${VERSION_INFO_RAW}
echo "${BASEURL}/${VERSION}/${SCRIPT}.bat" >>${VERSION_INFO_RAW}
echo "${EMPTY_HASH}" >>${VERSION_INFO_RAW}

echo "TESTERS" >> ${VERSION_INFO_RAW}
echo "1" >> ${VERSION_INFO_RAW}
${DIR_ALL}/${WORK_DIR}/unit_test.sh tools.ReleaseQuality ${RELEASE_XML_FILE} >>${VERSION_INFO_RAW}
cat ${TESTER_NAME_FILE} | head -1 >> ${VERSION_INFO_RAW}
echo "${EMPTY_PKHASH}" >>  ${VERSION_INFO_RAW}
echo "${TESTS_LIST}" >>  ${VERSION_INFO_RAW}
echo "${QOT}" >>  ${VERSION_INFO_RAW}
echo "${ROT}" >>  ${VERSION_INFO_RAW}
echo "${EMPTY_SIGN}" >>  ${VERSION_INFO_RAW}
echo "STOP" >>${VERSION_INFO_RAW}

echo "${DIR_ALL}/${WORK_DIR}/unit_test.sh  tools.UpdatesSigning ${DIR_ALL}/${VERSION}/${VERSION_INFO_RAW} ${DIR_ALL}/${TRUSTED_KEY} ${DIR_ALL}/${VERSION}/${VERSION_INFO_SIGNED} ${DIR_ALL}/${VERSION}"

${DIR_ALL}/${WORK_DIR}/unit_test.sh  tools.UpdatesSigning ${DIR_ALL}/${VERSION}/${VERSION_INFO_RAW} ${DIR_ALL}/${TRUSTED_KEY} ${DIR_ALL}/${VERSION}/${VERSION_INFO_SIGNED} ${DIR_ALL}/${VERSION}


echo -n ${VERSION} > ../HISTORY.`head ${VERSION_INFO_SIGNED} |head -3|tail -1|sed "s/[[:space:]]//g"`
echo -n ${VERSION} > ../LATEST

HISTORY=HISTORY.`head ${VERSION_INFO_SIGNED} |head -3|tail -1|sed "s/[[:space:]]//g"`
cd ..
tar cvjf ddp2p_${VERSION}.tar.bz2 LATEST ${HISTORY} dd_reinstall_DB.* dd_p2p*.sh dd_p2p*.bat ${VERSION}/update.* ${VERSION}/ddp2p_${VERSION}.jar README
zip -A -r ddp2p_${VERSION}.zip LATEST ${HISTORY} dd_reinstall_DB.* dd_p2p*.sh dd_p2p*.bat ${VERSION}/update.* ${VERSION}/ddp2p_${VERSION}.jar README


