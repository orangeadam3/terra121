package io.github.terra121.projection;

public class EqualEarth extends GeographicProjection {
	
	private int newton = 5;
	private static final double TO_RADIANS = Math.PI/180.0;
	
	public double[] toGeo(double x, double y) {
		
		double theta = y/A1; //start with initial guess at y/A1 since A1 is by far the largest term
		
		//Using newtons method to find theta
		for(int i=0; i<newton;i++) {
			double tpow = theta;
			
			//calculate a pseudo-y - goal and pseduo-dy/dt at theta to use newtons method root finding
			double pdy= A1; //A1
			double py= A1 * tpow - y; //A1 t - goal
			pdy += 3*A2*(tpow *= theta); //3 A2 t^2
			py += A2*(tpow *= theta); //A2 t^3
			pdy += 7*A3*(tpow *= theta*theta*theta); //7 A3 t^6
			py += A3*(tpow *= theta); //A3 t^7
			pdy += 9*A4*(tpow *= theta); //9 A4 t^8
			py += A4*(tpow *= theta); //A4 t^9
			
			//x = dx/dy
			theta -= py/pdy;
		}
		
		double thetasquare = theta*theta;
		double tpow = thetasquare;
		
		//recalc x denomenator to solve for lon
		double dx = A1; //A1
		dx += 3*A2*tpow; //3 A2 t^2
		dx += 7*A3*(tpow *= thetasquare*thetasquare); //7 A3 t^6
		dx += 9*A4*(tpow *= thetasquare); //9 A4 t^8
		
		return new double[] {x*dx*3/(TO_RADIANS*2*ROOT3*Math.cos(theta)), 
							Math.asin(Math.sin(theta)*2/ROOT3)/TO_RADIANS};
	}
	
	private static final double ROOT3 = Math.sqrt(3);
	
	private static final double A1 = 1.340264;
	private static final double A2 = -0.081106;
	private static final double A3 = 0.000893;
	private static final double A4 = 0.003796;
	
	public double[] fromGeo(double lon, double lat) {
		double sintheta = ROOT3*Math.sin(lat*TO_RADIANS)/2;
		double theta = Math.asin(sintheta);
		double tpow = theta;
		
		double x= A1; //A1
		double y= A1 * tpow; //A1 t
		x += 3*A2*(tpow *= theta); //3 A2 t^2
		y += A2*(tpow *= theta); //A2 t^3
		x += 7*A3*(tpow *= theta*theta*theta); //7 A3 t^6
		y += A3*(tpow *= theta); //A3 t^7
		x += 9*A4*(tpow *= theta); //9 A4 t^8
		y += A4*(tpow *= theta); //A4 t^9
		
		double costheta = Math.sqrt(1-sintheta*sintheta);
		
		return new double[] {(2*ROOT3*TO_RADIANS*lon*costheta/3)/x, y};
	}
	
	public double metersPerUnit() {
		return EARTH_CIRCUMFERENCE/(2*bounds()[2]);
	}
}
