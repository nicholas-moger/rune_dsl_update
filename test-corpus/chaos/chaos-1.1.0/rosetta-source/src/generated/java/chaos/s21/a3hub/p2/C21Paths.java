package chaos.s21.a3hub.p2;

import chaos.s21.a3hub.p2.meta.C21PathsMeta;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * only-exists targets.
 * @version 1.0.0
 */
@RosettaDataType(value="C21Paths", builder=C21Paths.C21PathsBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C21Paths", model="chaos", builder=C21Paths.C21PathsBuilderImpl.class, version="1.0.0")
public interface C21Paths extends RosettaModelObject {

	C21PathsMeta metaData = new C21PathsMeta();

	/*********************** Getter Methods  ***********************/
	String getP();
	String getQ();
	String getR();

	/*********************** Build Methods  ***********************/
	C21Paths build();
	
	C21Paths.C21PathsBuilder toBuilder();
	
	static C21Paths.C21PathsBuilder builder() {
		return new C21Paths.C21PathsBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C21Paths> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C21Paths> getType() {
		return C21Paths.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("p"), String.class, getP(), this);
		processor.processBasic(path.newSubPath("q"), String.class, getQ(), this);
		processor.processBasic(path.newSubPath("r"), String.class, getR(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C21PathsBuilder extends C21Paths, RosettaModelObjectBuilder {
		C21Paths.C21PathsBuilder setP(String p);
		C21Paths.C21PathsBuilder setQ(String q);
		C21Paths.C21PathsBuilder setR(String r);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("p"), String.class, getP(), this);
			processor.processBasic(path.newSubPath("q"), String.class, getQ(), this);
			processor.processBasic(path.newSubPath("r"), String.class, getR(), this);
		}
		

		C21Paths.C21PathsBuilder prune();
	}

	/*********************** Immutable Implementation of C21Paths  ***********************/
	class C21PathsImpl implements C21Paths {
		private final String p;
		private final String q;
		private final String r;
		
		protected C21PathsImpl(C21Paths.C21PathsBuilder builder) {
			this.p = builder.getP();
			this.q = builder.getQ();
			this.r = builder.getR();
		}
		
		@Override
		@RosettaAttribute("p")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("p")
		public String getP() {
			return p;
		}
		
		@Override
		@RosettaAttribute("q")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("q")
		public String getQ() {
			return q;
		}
		
		@Override
		@RosettaAttribute("r")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("r")
		public String getR() {
			return r;
		}
		
		@Override
		public C21Paths build() {
			return this;
		}
		
		@Override
		public C21Paths.C21PathsBuilder toBuilder() {
			C21Paths.C21PathsBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C21Paths.C21PathsBuilder builder) {
			ofNullable(getP()).ifPresent(builder::setP);
			ofNullable(getQ()).ifPresent(builder::setQ);
			ofNullable(getR()).ifPresent(builder::setR);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C21Paths _that = getType().cast(o);
		
			if (!Objects.equals(p, _that.getP())) return false;
			if (!Objects.equals(q, _that.getQ())) return false;
			if (!Objects.equals(r, _that.getR())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (p != null ? p.hashCode() : 0);
			_result = 31 * _result + (q != null ? q.hashCode() : 0);
			_result = 31 * _result + (r != null ? r.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C21Paths {" +
				"p=" + this.p + ", " +
				"q=" + this.q + ", " +
				"r=" + this.r +
			'}';
		}
	}

	/*********************** Builder Implementation of C21Paths  ***********************/
	class C21PathsBuilderImpl implements C21Paths.C21PathsBuilder {
	
		protected String p;
		protected String q;
		protected String r;
		
		@Override
		@RosettaAttribute("p")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("p")
		public String getP() {
			return p;
		}
		
		@Override
		@RosettaAttribute("q")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("q")
		public String getQ() {
			return q;
		}
		
		@Override
		@RosettaAttribute("r")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("r")
		public String getR() {
			return r;
		}
		
		@RosettaAttribute("p")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("p")
		@Override
		public C21Paths.C21PathsBuilder setP(String _p) {
			this.p = _p == null ? null : _p;
			return this;
		}
		
		@RosettaAttribute("q")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("q")
		@Override
		public C21Paths.C21PathsBuilder setQ(String _q) {
			this.q = _q == null ? null : _q;
			return this;
		}
		
		@RosettaAttribute("r")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("r")
		@Override
		public C21Paths.C21PathsBuilder setR(String _r) {
			this.r = _r == null ? null : _r;
			return this;
		}
		
		@Override
		public C21Paths build() {
			return new C21Paths.C21PathsImpl(this);
		}
		
		@Override
		public C21Paths.C21PathsBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C21Paths.C21PathsBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getP()!=null) return true;
			if (getQ()!=null) return true;
			if (getR()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C21Paths.C21PathsBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C21Paths.C21PathsBuilder o = (C21Paths.C21PathsBuilder) other;
			
			
			merger.mergeBasic(getP(), o.getP(), this::setP);
			merger.mergeBasic(getQ(), o.getQ(), this::setQ);
			merger.mergeBasic(getR(), o.getR(), this::setR);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C21Paths _that = getType().cast(o);
		
			if (!Objects.equals(p, _that.getP())) return false;
			if (!Objects.equals(q, _that.getQ())) return false;
			if (!Objects.equals(r, _that.getR())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (p != null ? p.hashCode() : 0);
			_result = 31 * _result + (q != null ? q.hashCode() : 0);
			_result = 31 * _result + (r != null ? r.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C21PathsBuilder {" +
				"p=" + this.p + ", " +
				"q=" + this.q + ", " +
				"r=" + this.r +
			'}';
		}
	}
}
