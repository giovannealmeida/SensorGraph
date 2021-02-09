package br.com.giovanne.sensorgraph;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import java.text.DecimalFormat;

/**
 * Created by Giovanne Almeida on 11/01/2021
 */
public class SensorGraph extends View {
    private static final String TAG = "SensorGraph";
    private static final float LINE_STROKE_WIDTH = 4;

    private final float SUP_LIMIT_LINE = 30; //Linha de escala superior
    private float SUB_LIMIT_LINE; //Linha de escala inferior
    private float supLimitValue; //Valor da escala superior
    private float subLimitValue; //Valor da escala inferior

    private float oY; //Posição da origem do eixo Y / posição da linha do eixo X

    //Paths que representam os valores das coordenadas
    private Path[] mPathsX;
    private Path[] mPathsY;
    private Path[] mPathsZ;

    //Paints dos paths das coordenadas
    private Paint mPaintX;
    private Paint mPaintY;
    private Paint mPaintZ;
    //Paint das linhas dos eixos
    private Paint mPaintAxis;
    //Paint para os textos/valores no gráfico
    private Paint mPaintText;
    //Paint para as bordas do gráfico
    private Paint mPaintBorders;
    //Incremento global para disposição dos pontos ao longo do eixo X
    private float globalXCounter;
    //Valor máximo de pontos visíveis no gráfico
    private int mMaxAmountValues;
    //A distância entre um ponto e outro no eixo X. O próximo ponto estará à distância de mXOffset pixels do anterior.
    private float mXOffset = 0;
    //Autura e largura da área do gráfico
    private int mWidth, mHeight;
    //Valor multiplicado pelos valores das coordenadas a fim de melhorar a visibiidade do ponto plotado
    private float factor = 1000;
    //Cor de fundo do gráfico
    private int backgroundColor;
    //Determina se o gráfico já está se movendo para a esquerda (os pontos preencheram toda a largura do gráfico)
    private boolean isScrolling;

    public SensorGraph(Context context) {
        super(context);
        init(null);
    }

    public SensorGraph(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public SensorGraph(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public SensorGraph(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    /**
     * Initilize resources
     * @param attrs Attributes set on the XML tag
     */
    private void init(AttributeSet attrs) {
        mPaintX = new Paint();
        mPaintX.setColor(Color.RED);
        mPaintX.setStyle(Paint.Style.STROKE);
        mPaintX.setStrokeWidth(LINE_STROKE_WIDTH);
        mPaintY = new Paint();
        mPaintY.setColor(Color.GREEN);
        mPaintY.setStyle(Paint.Style.STROKE);
        mPaintY.setStrokeWidth(LINE_STROKE_WIDTH);
        mPaintZ = new Paint();
        mPaintZ.setColor(Color.BLUE);
        mPaintZ.setStyle(Paint.Style.STROKE);
        mPaintZ.setStrokeWidth(LINE_STROKE_WIDTH);

        globalXCounter = 0;
        mPaintAxis = new Paint();
        mPaintAxis.setStyle(Paint.Style.STROKE);

        mPaintText = new Paint();
        mPaintText.setTextSize(22);

        mPaintBorders = new Paint();
        mPaintBorders.setStyle(Paint.Style.STROKE);

        //Existem 2 paths para cada coordenada. O path[1] é o path que está sendo desenhado no memomento,
        //o path[0] é o path que está marcado para ser descaratado ao sair da tela.
        //Quando o path[0] sair totalmente da tela, ele é substituído pelo path[1], ao passo que o path[1]
        //recomeça a ser escrito. Esta estratégia garante que os paths não cresçam infinitamente.
        mPathsX = new Path[2];
        mPathsX[1] = new Path();
        mPathsY = new Path[2];
        mPathsY[1] = new Path();
        mPathsZ = new Path[2];
        mPathsZ[1] = new Path();

        getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            mWidth = getWidth();
            mHeight = getHeight();
            recalculateXOffset();
            oY = mHeight / 2f;
            SUB_LIMIT_LINE = mHeight - SUP_LIMIT_LINE;
        });

        if (attrs != null) {
            TypedArray t = getContext().obtainStyledAttributes(attrs, R.styleable.SensorGraph);
            backgroundColor = t.getColor(R.styleable.SensorGraph_backgroundColor, Color.parseColor("#ffffcc"));
            mPaintBorders.setStrokeWidth(t.getDimensionPixelSize(R.styleable.SensorGraph_borderWidth, 0));
            mPaintBorders.setColor(t.getColor(R.styleable.SensorGraph_borderColor, Color.parseColor("#000000")));

            mPaintX.setStrokeWidth(t.getDimension(R.styleable.SensorGraph_valuesLineWidth, LINE_STROKE_WIDTH));
            mPaintY.setStrokeWidth(t.getDimension(R.styleable.SensorGraph_valuesLineWidth, LINE_STROKE_WIDTH));
            mPaintZ.setStrokeWidth(t.getDimension(R.styleable.SensorGraph_valuesLineWidth, LINE_STROKE_WIDTH));

            mPaintAxis.setStrokeWidth(t.getDimension(R.styleable.SensorGraph_axisLineWidth, 1));
            mPaintText.setTextSize(t.getDimension(R.styleable.SensorGraph_valuesTextSize, 22));
            mPaintText.setColor(t.getColor(R.styleable.SensorGraph_valuesTextColor, Color.parseColor("#000000")));
        }
    }

    /**
     * Recalcula a distância entre os pontos.
     * Sempre que o usuário alterar a quantidade máxima de valores visívieis no gráfico, esse valor
     * tem que ser atualizado.
     */
    private void recalculateXOffset() {
        mXOffset = (float) mWidth / mMaxAmountValues;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //background
        canvas.drawColor(backgroundColor);
        //Y axis
        canvas.drawLine(0, oY, mWidth, oY, mPaintAxis);

        //paths
        if (mPathsZ[0] != null) //num primeiro momento ainda não vai existir a porção descartável do path
            canvas.drawPath(mPathsZ[0], mPaintZ);
        canvas.drawPath(mPathsZ[1], mPaintZ);

        if (mPathsY[0] != null)
            canvas.drawPath(mPathsY[0], mPaintY);
        canvas.drawPath(mPathsY[1], mPaintY);

        if (mPathsX[0] != null) {
            canvas.drawPath(mPathsX[0], mPaintX);
        }
        canvas.drawPath(mPathsX[1], mPaintX);

        drawLimitLines(canvas);
        //bordas
        canvas.drawRect(mPaintBorders.getStrokeWidth() / 2, mPaintBorders.getStrokeWidth(), mWidth - mPaintBorders.getStrokeWidth() / 2, mHeight - mPaintBorders.getStrokeWidth(), mPaintBorders);
    }

    /**
     * Draws the superior and inferior limit lines.
     *
     * @param canvas Canvas where the lines will be drawn.
     */
    private void drawLimitLines(Canvas canvas) {
        supLimitValue = -(SUP_LIMIT_LINE - oY) / factor;
        subLimitValue = -(SUB_LIMIT_LINE - oY) / factor;

        canvas.drawLine(0, SUP_LIMIT_LINE, mWidth, SUP_LIMIT_LINE, mPaintAxis);
        canvas.drawLine(0, SUB_LIMIT_LINE, mWidth, SUB_LIMIT_LINE, mPaintAxis);
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(3);
        canvas.drawText(df.format(supLimitValue), 10, SUP_LIMIT_LINE + mPaintText.getTextSize() + 6, mPaintText);
        canvas.drawText(df.format(subLimitValue), 10, SUB_LIMIT_LINE - 10, mPaintText);
    }

    public void addPoints(float x, float y, float z) {
        drawLines(convertToYAxis(x), mPathsX);
        drawLines(convertToYAxis(y), mPathsY);
        drawLines(convertToYAxis(z), mPathsZ);
        if (!isScrolling)
            globalXCounter += mXOffset;
        invalidate();
    }

    /**
     * Converts the given value to a Y-value to be placed on the graph.
     *
     * @param value Value to be converted.
     * @return Value on the Y axis.
     */
    private Float convertToYAxis(float value) {
        Float yValue = oY - value * factor;
        // Se o gráfico passou por cima ou por baixo, reajusta o factor
        updateFactor(value, yValue);
        return yValue;
    }

    /**
     * Draw the lines in the path
     *
     * @param valueY     Next Y-point to be added at the end of the line.
     * @param paths      Paths containing the lines to be drawn.
     */
    private void drawLines(Float valueY, Path[] paths) {
        paths[1].lineTo(globalXCounter, valueY);

        //Verifica a área do caminho
        RectF bounds = new RectF();
        paths[1].computeBounds(bounds, true);

        //O caminho chegou na direita da tela ecomeçou a andar pra esquerda da tela
        if (bounds.right >= mWidth) {
            isScrolling = true;
            if (paths[0] != null)
                paths[0].offset(-mXOffset, 0);
            paths[1].offset(-mXOffset, 0);

            //O caminho atual saiu da tela pela esquerda
            if (bounds.left <= 0) {
                paths[0] = paths[1];
                paths[1] = new Path();
                paths[1].moveTo(globalXCounter - mXOffset, valueY);
            }
        }
    }

    //Verifica se o gráfico passa dos limites superior ou inferior e ajusta o factor
    private void updateFactor(float value, Float valueY) {
        if (valueY <= SUP_LIMIT_LINE || valueY >= SUB_LIMIT_LINE) {
            factor = Math.abs((oY - SUP_LIMIT_LINE) / Math.abs(value));
        }
    }

    public void setMaxAmountValues(int maxAmountValues) {
        this.mMaxAmountValues = maxAmountValues;
        recalculateXOffset();
    }
}
