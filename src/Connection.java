interface Connection {
    public float getValue();
    public FiringState getFired();
    public void setNetwork(Network net);
    public Connection clone(Network net);
}