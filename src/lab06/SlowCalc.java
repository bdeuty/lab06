package lab06;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class SlowCalc extends JFrame {
    private int numThreads;
    private int numToCalc;
    private JLabel threadLabel = new JLabel("Number of threads: ");
    private JLabel numbLabel = new JLabel("Number to calculate :");
    private JLabel calcLabel = new JLabel("Calculation Area");
    private JButton startButton = new JButton("Start Calculation");
    private JButton cancelButton = new JButton("Cancel");
    private JTextField inputThreadField = new JTextField(5);
    private JTextField inputNumbField = new JTextField(10);

    private boolean cancelRequested = false;  // Flag to stop threads
    private long startTime;  // Start time for the timer
    private long endTime;    // End time for the timer

    private void updateTextField() {
        inputNumbField.setText("");
        inputThreadField.setText("");
        validate();
    }

    public SlowCalc() {
        setTitle("Find Prime Numbers");
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setVisible(true);

        JPanel inputPanel = new JPanel();
        inputPanel.add(threadLabel);
        inputPanel.add(inputThreadField);
        inputPanel.add(numbLabel);
        inputPanel.add(inputNumbField);
        inputPanel.add(cancelButton);

        JPanel startPanel = new JPanel();
        startPanel.add(startButton);

        JPanel calcPanel = new JPanel();
        calcPanel.add(calcLabel);

        add(inputPanel, BorderLayout.NORTH);
        add(startPanel, BorderLayout.SOUTH);
        add(calcPanel, BorderLayout.CENTER);

        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Start Button Clicked!");
                startCalc();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Cancel Button clicked!");
                cancelCalc();
            }
        });
    }

    private void startCalc() {
        System.out.println("Calculation Started");
        try {
            numThreads = Integer.parseInt(inputThreadField.getText());  // Read number of threads from text field
            int number = Integer.parseInt(inputNumbField.getText());    // Read the number to calculate primes up to
            startTime = System.currentTimeMillis();  // Start the timer
            cancelRequested = false;  // Reset cancel flag

            // Run the prime calculation in a background thread using SwingWorker
            PrimeCalculationWorker primeWorker = new PrimeCalculationWorker(number, numThreads);
            primeWorker.execute();  // Start the worker thread

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers.");
        }
        updateTextField();
    }

    private void cancelCalc() {
        System.out.println("Calculation canceled");
        cancelRequested = true;  // Set the cancel flag
        endTime = System.currentTimeMillis();  // Stop the timer
        long elapsedTime = endTime - startTime;  // Calculate elapsed time

        // Show a message that the calculation is canceled
        calcLabel.setText("<html>Calculation canceled.<br>Elapsed time: " + elapsedTime + " ms.<br>Found primes so far: " + calcLabel.getText() + "</html>");
    }

    // SwingWorker for performing prime calculation
    public class PrimeCalculationWorker extends SwingWorker<Void, String> {
        private int number;
        private int numThreads;

        public PrimeCalculationWorker(int number, int numThreads) {
            this.number = number;
            this.numThreads = numThreads;
        }

        @Override
        protected Void doInBackground() throws Exception {
            checkPrimes(number, numThreads);  // Perform the prime check in the background
            return null;
        }

        @Override
        protected void process(List<String> chunks) {
            // Update the calcLabel periodically with found primes
            for (String chunk : chunks) {
                calcLabel.setText(chunk);
            }
        }

        @Override
        protected void done() {
            // When done, calculate the elapsed time and update the label
            endTime = System.currentTimeMillis();  // Stop the timer
            long elapsedTime = endTime - startTime;  // Calculate elapsed time

            if (cancelRequested) {
                calcLabel.setText("<html>Calculation canceled.<br>Elapsed time: " + elapsedTime + " ms.<br>Found primes so far: " + calcLabel.getText() + "</html>");
            } else {
                calcLabel.setText("<html>Calculation completed.<br>Elapsed time: " + elapsedTime + " ms.<br>Found primes: " + calcLabel.getText() + "</html>");
            }
        }
    }

    // This is where we perform the actual prime checking, now inside SwingWorker
    public void checkPrimes(int number, int numThreads) {
        final int[] numPrime = {0};  // To store the number of primes found
        StringBuilder primeNumbers = new StringBuilder("<html>");

        // Divide the workload among threads
        int rangePerThread = number / numThreads;

        // Create and start threads
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            int start = i * rangePerThread + 2;
            int end = (i == numThreads - 1) ? number : (i + 1) * rangePerThread;

            threads[i] = new PrimeWorker(start, end, numPrime, primeNumbers);
            threads[i].start();
        }

        // Wait for all threads to finish or be canceled
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (!cancelRequested) {
            // Calculation finished without cancellation, send the result to SwingWorker's `process` method
            publish(primeNumbers.toString());  // `publish` is now called in the context of SwingWorker
        }
    }

    // Worker thread to check primes in a specific range
    public class PrimeWorker extends Thread {
        private int start;
        private int end;
        private final int[] numPrime;
        private final StringBuilder primeNumbers;

        public PrimeWorker(int start, int end, int[] numPrime, StringBuilder primeNumbers) {
            this.start = start;
            this.end = end;
            this.numPrime = numPrime;
            this.primeNumbers = primeNumbers;
        }

        @Override
        public void run() {
            for (int i = start; i < end; i++) {
                if (cancelRequested) {
                    break;  // Exit early if cancel is requested
                }

                boolean isPrime = true;
                for (int n = 2; n < i; n++) {
                    if (i % n == 0) {
                        isPrime = false;
                        break;
                    }
                }
                if (isPrime) {
                    synchronized (primeNumbers) {
                        primeNumbers.append(i).append(" ");
                    }
                    synchronized (numPrime) {
                        numPrime[0]++;
                    }
                }
            }

            // Periodically update the UI with the current prime numbers
            String currentPrimes = primeNumbers.toString() + "</html>";
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    publish(currentPrimes);  // Call publish from within SwingWorker context
                }
            });
        }
    }

    public static void main(String[] args) {
        new SlowCalc();
    }
}
