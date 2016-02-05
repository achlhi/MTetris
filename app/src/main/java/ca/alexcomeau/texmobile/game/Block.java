package ca.alexcomeau.texmobile.game;

import android.graphics.Point;
import android.os.Parcel;
import android.os.Parcelable;

public class Block implements Parcelable {
    private int rotation;
    private Point position;
    private Shape shape;

    // Block types
    public enum Shape{
        //[][][][]
        I(new Point[][]{
                {new Point(0,2),new Point(1,2),new Point(2,2),new Point(3,2)},
                {new Point(2,3),new Point(2,2),new Point(2,1),new Point(2,0)}
        }),
        //[][][]
        //    []
        J(new Point[][]{
                {new Point(0,2),new Point(1,2),new Point(2,2),new Point(2,1)},
                {new Point(1,3),new Point(1,2),new Point(1,1),new Point(0,1)},
                {new Point(0,2),new Point(0,1),new Point(1,1),new Point(2,1)},
                {new Point(1,3),new Point(2,3),new Point(1,2),new Point(1,1)}
        }),
        //[][][]
        //[]
        L(new Point[][]{
                {new Point(0,2),new Point(1,2),new Point(2,2),new Point(0,1)},
                {new Point(1,3),new Point(0,3),new Point(1,2),new Point(1,1)},
                {new Point(2,2),new Point(0,1),new Point(1,1),new Point(2,1)},
                {new Point(1,3),new Point(1,2),new Point(1,1),new Point(2,1)}
        }),
        //[][]
        //[][]
        O(new Point[][]{
                {new Point(1,2),new Point(2,2),new Point(1,1),new Point(2,1)}
        }),
        //  [][]
        //[][]
        S(new Point[][]{
                {new Point(1,2),new Point(2,2),new Point(1,1),new Point(0,1)},
                {new Point(0,3),new Point(0,2),new Point(1,2),new Point(1,1)}
        }),
        //[][][]
        //  []
        T(new Point[][]{
                {new Point(0,2),new Point(1,2),new Point(2,2),new Point(1,1)},
                {new Point(1,3),new Point(1,2),new Point(0,2),new Point(1,1)},
                {new Point(1,2),new Point(0,1),new Point(1,1),new Point(2,1)},
                {new Point(1,3),new Point(1,2),new Point(2,2),new Point(1,1)}
        }),
        //[][]
        //  [][]
        Z(new Point[][]{
                {new Point(0,2),new Point(1,2),new Point(1,1),new Point(2,1)},
                {new Point(2,3),new Point(1,2),new Point(2,2),new Point(1,1)}
        });

        private Point[][] rotations;
        Shape(Point[][] rot) { rotations = rot; }
        public Point[][] getRotations() { return rotations; }
    }

    public Block(Point start, Shape shape)
    {
        rotation = 0;
        position = new Point(start);
        this.shape = shape;
    }

    public void moveUp() { position.y++; }
    public void moveDown() { position.y--; }
    public void moveLeft() { position.x--; }
    public void moveRight() { position.x++; }

    // Loops around the rotations clockwise
    public void rotateRight()
    {
        rotation++;
        if(rotation == shape.getRotations().length)
            rotation = 0;
    }

    // Loops around the rotations counterclockwise
    public void rotateLeft()
    {
        rotation--;
        if(rotation == -1)
            rotation = shape.getRotations().length - 1;
    }

    // Adds the position to the current relative coordinates and returns that.
    public Point[] getAbsoluteCoordinates()
    {
        Point[] relative = getRelativeCoordinates();
        Point[] coords = new Point[relative.length];

        for(int i = 0; i < relative.length; i++)
            coords[i] = new Point(relative[i].x + position.x, relative[i].y + position.y);

        return coords;
    }

    public Shape getShape() { return shape; }
    public Point getPosition() { return position; }
    public Point[] getRelativeCoordinates() { return shape.getRotations()[rotation]; }

    // ===== Parcelable Stuff ====================================================
    protected Block(Parcel in) {
        rotation = in.readInt();
        position = (Point) in.readValue(Point.class.getClassLoader());
        shape = (Shape) in.readSerializable();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(rotation);
        dest.writeValue(position);
        dest.writeSerializable(shape);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Block> CREATOR = new Parcelable.Creator<Block>() {
        @Override
        public Block createFromParcel(Parcel in) {
            return new Block(in);
        }

        @Override
        public Block[] newArray(int size) {
            return new Block[size];
        }
    };
}