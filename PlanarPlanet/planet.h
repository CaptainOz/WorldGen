#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <time.h>
#include <GL/glut.h>

/* 
	patched for Windows compatibility
	by Keri Matthews
*/
#ifndef TRUE
 #define TRUE 1
 #define FALSE 0
#endif
#ifdef WIN32 || NEED_RAND48
 #define drand48() ((double)rand()/(double)RAND_MAX)
 #define srand48(i) srand(i)
#endif

typedef struct {
   double x,y,z;
} XYZ;

typedef struct {
   double r,g,b;
} COLOUR;

typedef struct {
   XYZ p[3];   /* Vertices */
   int c[3];   /* Hight Counts */
} TF;

typedef struct {
   XYZ vp;              /* View position           */
   XYZ vd;              /* View direction vector   */
   XYZ vu;              /* View up direction       */
   XYZ pr;              /* Point to rotate about   */
   double focallength;  /* Focal Length along vd   */
   double aperture;     /* Camera aperture         */
   double eyesep;       /* Eye separation          */
   int screenwidth,screenheight;
} CAMERA;

#define DTOR            0.0174532925
#define RTOD            57.2957795
#define TWOPI           6.283185307179586476925287
#define PI              3.141592653589793238462643
#define PID2            1.570796326794896619231322
#define ESC 27
#define CROSSPROD(p1,p2,p3) \
   p3.x = p1.y*p2.z - p1.z*p2.y; \
   p3.y = p1.z*p2.x - p1.x*p2.z; \
   p3.z = p1.x*p2.y - p1.y*p2.x
#define DOTPRODUCT(v1,v2) ( v1.x*v2.x + v1.y*v2.y + v1.z*v2.z )
#define ABS(x) (x < 0 ? -(x) : (x))
#define MIN(x,y) (x < y ? x : y)
#define MAX(x,y) (x > y ? x : y)

void Display(void);
void CreateEnvironment(void);
void CreateInitialPlanet(void);
void CalcBounds(void);
void MakeGeometry(void);
void DrawHistogram(void);
void MakeLighting(void);
void HandleKeyboard(unsigned char key,int x, int y);
void HandleSpecialKeyboard(int key,int x, int y);
void HandleMouse(int,int,int,int);
void HandleMainMenu(int);
void HandleIterMenu(int);
void HandleMethodMenu(int);
void HandleResolMenu(int);
void HandleHeightMenu(int);
void HandleColourMenu(int);
void HandleVisibility(int vis);
void HandleReshape(int,int);
void HandleMouseMotion(int,int);
void HandlePassiveMotion(int,int);
void HandleTimer(int);
void GiveUsage(char *);
void RotateCamera(int,int,int);
void TranslateCamera(int,int);
void CameraHome(int);
void FlyCamera(int);
int  FormSphere(int);
int  MakeNSphere(TF *,int);

double DotProduct(XYZ,XYZ);
double Modulus(XYZ);
void   Normalise(XYZ *);
XYZ    VectorSub(XYZ,XYZ);
XYZ    VectorAdd(XYZ,XYZ);
XYZ    MidPoint(XYZ,XYZ);
COLOUR GetColour(double,double,double,int);
int WindowDump(int,int,int);
void CreateSimpleSphere(XYZ,double,int,int);
