/* Copyright (C) 2014,2015 Authors: Hang Dong <hdong2012@my.fit.edu>, Marius Silaghi <silaghi@fit.edu>
Florida Tech, Human Decision Support Systems Laboratory
This program is free software; you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation; either the current version of the License, or
(at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.
You should have received a copy of the GNU Affero General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA. */
/* ------------------------------------------------------------------------- */

package com.HumanDecisionSupportSystemsLaboratory.DD_P2P;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MyTime {
	public static String geTimeNoS(){
		Date date=new Date();   
		SimpleDateFormat df=new SimpleDateFormat("MM-dd HH:mm");   
		String time=df.format(date);
		return time;
	}
	public static String geTime(){
		Date date=new Date();   
		SimpleDateFormat df=new SimpleDateFormat("MM-dd HH:mm:ss");   
		String time=df.format(date);
		return time;
	}
}
