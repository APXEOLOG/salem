package org.apxeolog.salem;

import java.io.Serializable;

public class Pair<TYPEA, TYPEB> implements Comparable<Pair<TYPEA, TYPEB>>, Serializable {
	private static final long serialVersionUID = -8574256111175565367L;
	
	protected final TYPEA Key_;
	protected final TYPEB Value_;

	public Pair(TYPEA key, TYPEB value) {
		Key_ = key;
		Value_ = value;
	}

	public TYPEA getKey() {
		return Key_;
	}

	public TYPEB getValue() {
		return Value_;
	}

	public String toString() {
		StringBuffer buff = new StringBuffer();
		buff.append("Key: ");
		buff.append(Key_);
		buff.append("\tValue: ");
		buff.append(Value_);
		return (buff.toString());
	}

	public int compareTo(Pair<TYPEA, TYPEB> p1) {
		if (null != p1) {
			if (p1.equals(this)) {
				return 0;
			} else if (p1.hashCode() > this.hashCode()) {
				return 1;
			} else if (p1.hashCode() < this.hashCode()) {
				return -1;
			}
		}
		return (-1);
	}

	public boolean equals(Pair<TYPEA, TYPEB> p1) {
		if (null != p1) {
			if (p1.Key_.equals(this.Key_) && p1.Value_.equals(this.Value_)) {
				return (true);
			}
		}
		return (false);
	}

	public int hashCode() {
		int hashCode = Key_.hashCode() + (31 * Value_.hashCode());
		return (hashCode);
	}
}