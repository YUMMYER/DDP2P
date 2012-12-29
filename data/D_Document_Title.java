/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Marius C. Silaghi
		Author: Marius Silaghi: msilaghi@fit.edu
		Florida Tech, Human Decision Support Systems Laboratory
   
       This program is free software; you can redistribute it and/or modify
       it under the terms of the GNU Affero General Public License as published by
       the Free Software Foundation; either the current version of the License, or
       (at your option) any later version.
   
      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.
  
      You should have received a copy of the GNU Affero General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.              */
/* ------------------------------------------------------------------------- */

package data;

import java.util.Calendar;


import config.DD;

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

/**
TitleDocument ::= Document
 */

public class D_Document_Title extends ASNObj{
	
	public D_Document title_document;
	public D_Document_Title(){
		title_document = new D_Document();
	}
	
	public String toString() {
		return "TD:"+title_document;
	}
	
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		if(title_document!=null)enc.addToSequence(title_document.getEncoder().setASN1Type(DD.TAG_AC0));
		return enc;
	}

	@Override
	public D_Document_Title decode(Decoder decoder) throws ASN1DecoderFail {

		Decoder dec = decoder.getContent();
		if(dec.getTypeByte()==DD.TAG_AC0)title_document = new D_Document().decode(dec.getFirstObject(true));
		return this;
	}
	
}
