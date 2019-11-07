#!/usr/bin/env bash

DIST=trusty
CRAN_DIST=
ARCH=amd64
BUILD_PATH=./dist
WS=`dirname $0`

BQ_PATH=../extras/bigquery/
IMPALA_PATH=../extras/impala/
NETEZZA_PATH=../extras/netezza/
HIVE_PATH=../extras/hive/

function print_help {
	echo "Usage: run_build.sh [OPTIONS]"
	echo "Available options are:"
	echo -e "  -a i386|amd64 \tDistribution architecture, default is amd64"
	echo -e "  -d DIST_NAME \t\tUbuntu distribution name, e.g. trusty or xenial, default is trusty"
	echo -e "  -r R_DIST_NAME \t\tUbuntu distribution name from cran with R packages, default is the same as used for DIST_NAME"
	echo -e "  -b BUILDDIR \t\tDirectory where distribution build would be running"
	echo -e "  -f FILE \t\tOutput archive filename"
	echo -e "  -g PATH \t\tPath to BigQuery drivers"
	echo -e "  -i PATH \t\tPath to Impala drivers"
	echo -e "  -n PATH \t\tPath to Netezza drivers"
	echo -e "  -v PATH \t\tPath to Hive drivers"
	echo -e "  -h \t\t\tPrints this"
}

OPTIND=1
while getopts ":a:d:b:f:h:g:i:n" opt; do
	case $opt in 
		a)
			ARCH=$OPTARG
			;;
		d)	
			DIST=$OPTARG
			;;
        r)
            CRAN_DIST=$OPTARG
            ;;
		b)
			BUILD_PATH=$OPTARG
			;;
		f)
			ARCHIVE=$OPTARG
			;;
		h)
			print_help
			exit 0
			;;
		g)
		    BQ_PATH=$OPTARG
		    ;;
		i)
		    IMPALA_PATH=$OPTARG
		    ;;
		n)
		    NETEZZA_PATH=$OPTARG
		    ;;
		\?)
			echo "Invalid option: -$OPTARG" >&2
			exit 1
			;;
		:)
			echo "Option -$OPTARG requires an argument." >&2
			exit 1
			;;
	esac
done

if [[ -z $CRAN_DIST ]]; then
  CRAN_DIST=$DIST
fi

if [[ -z $ARCHIVE ]]; then
	ARCHIVE=../r_base_${DIST}_${ARCH}.tar.gz
fi

if [[ ! -d $BUILD_PATH ]]; then
	mkdir -p $BUILD_PATH
fi

if [[ "$(ls -A $BUILD_PATH)" ]]; then
	echo "$BUILD_PATH is not empty, woun't continue"
	exit 1
fi

echo "Starting build distribution."
echo "Release: $DIST $ARCH"
echo "Build dir: $BUILD_PATH"
echo "Output file: $ARCHIVE"
echo ""

# Download libs.r from GitHub repo
if [[ -f "libs/libs_1.r" ]]; then
    rm -f "libs/libs*.r"
fi

mkdir libs
curl https://raw.githubusercontent.com/odysseusinc/DockerEnv/master/libs/libs_1.r -o libs/libs_1.r
curl https://raw.githubusercontent.com/odysseusinc/DockerEnv/master/libs/libs_2.r -o libs/libs_2.r
curl https://raw.githubusercontent.com/odysseusinc/DockerEnv/master/libs/libs_3.r -o libs/libs_3.r
curl https://raw.githubusercontent.com/odysseusinc/DockerEnv/master/libs/libs_4.r -o libs/libs_4.r
curl https://raw.githubusercontent.com/odysseusinc/DockerEnv/master/libs/libs_5.r -o libs/libs_5.r
curl https://raw.githubusercontent.com/odysseusinc/DockerEnv/master/libs/libs_6.r -o libs/libs_6.r

debootstrap --arch amd64 $DIST $BUILD_PATH http://ubuntu.cs.utah.edu/ubuntu/
mount --bind /proc $BUILD_PATH/proc

cp $WS/install_packages.sh $BUILD_PATH/root/
cp -r $WS/libs $BUILD_PATH/root

# Authorization token
if [[ -a "$HOME/.Renviron" ]]; then
    sudo cp $HOME/.Renviron $BUILD_PATH/root/
fi

#Impala drivers
mkdir $BUILD_PATH/impala/
cp $IMPALA_PATH/*.jar $BUILD_PATH/impala/
cp ../docker/krb5.conf $BUILD_PATH/etc/

# BigQuery drivers
mkdir $BUILD_PATH/bigquery/
cp $BQ_PATH/*.jar $BUILD_PATH/bigquery/

# Netezza drivers
mkdir $BUILD_PATH/netezza/
cp $NETEZZA_PATH/*.jar $BUILD_PATH/netezza/

# Hive drivers
mkdir $BUILD_PATH/hive/
cp $HIVE_PATH/*.jar $BUILD_PATH/hive/

sudo chmod +x $BUILD_PATH/root/install_packages.sh
sudo chroot $BUILD_PATH /root/install_packages.sh $CRAN_DIST

umount $BUILD_PATH/proc
sudo rm -f $BUILD_PATH/root/install_packages.sh
sudo rm -f $BUILD_PATH/root/libs.r
sudo rm -f $BUILD_PATH/root/.Renviron
# To prevent unexpected package updates
sudo cp $WS/.Rprofile $BUILD_PATH/root/

cd $BUILD_PATH
tar czf $ARCHIVE .
echo "Distribution Archive built and available at $ARCHIVE"