package foreign;

/** @noinspection CStyleArrayDeclaration */
public class FFTState {
	int size = 0;
	boolean inPlace = false;
	double ji[] = null;
	double jo[] = null;

	public void Setup(int size, boolean inPlace) {
		this.size = size;
		this.inPlace = inPlace;
		ji = new double[size * 2];
		if (inPlace)
			jo = ji;
		else
			jo = new double[size * 2];
		for (int i = 0; i < size; i++)
			ji[i] = Math.random() * 2 - 1.0;
	}
}
